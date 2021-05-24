package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import framework.core.Controller;
import framework.core.Game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class AutoSubgoalMCTS
{
    public int maxRolloutDepth = 25;

    private MCTSNode<SubgoalData> root;
    private ISubgoalSearch subgoalSearch;
    private Random rng;

    private RewardAccumulator rewardAccumulator;

    public double explorationRate = Math.sqrt(2);
    public int maxSimulationSteps = 100;

    public AutoSubgoalMCTS(ISubgoalSearch subgoalSearch, Random rng)
    {
        this.root = new MCTSNode<>(new SubgoalData());
        this.root.data.subgoalSearch = subgoalSearch.copy();
        this.subgoalSearch = subgoalSearch;
        this.rng = rng;

        rewardAccumulator = new RewardAccumulator(0.99);
    }

    public void step(RewardGame initialGame)
    {
        // Selection
        RewardGame game = initialGame.getCopy();
        MCTSNode<SubgoalData> currNode = root;
        int depth = 0;
        while (!game.isEnded() && currNode.data.subgoalSearch == null && depth < maxRolloutDepth)
        {
            currNode.data.lastSeenPosition = game.getState().getShip().s.copy();
            currNode = currNode.selectUCT(explorationRate, rng);
            rewardAccumulator.addReward(currNode.data.macroAction.apply(game));
            depth += currNode.data.macroAction.size();
        }
        currNode.data.lastSeenPosition = game.getState().getShip().s.copy();

        // Expansion
        if (!game.isEnded() && depth < maxRolloutDepth)
        {
            if(currNode.data.subgoalSearch.isDone())
            {
                for(MacroAction a : currNode.data.subgoalSearch.getMacroActions())
                {
                    SubgoalData newData = new SubgoalData();
                    newData.subgoalSearch = subgoalSearch.copy();
                    newData.macroAction = a;

                    currNode.addChild(newData);
                }
                currNode.data.subgoalSearch = null;

                // Execute one macro action
                currNode = currNode.children.get(0);
                rewardAccumulator.addReward(currNode.data.macroAction.apply(game));
                depth += currNode.data.macroAction.size();
            }
            else
            {
                double rewardBefore = game.getRewardSum();
                depth += currNode.data.subgoalSearch.step(game);
                rewardAccumulator.addReward(game.getRewardSum() - rewardBefore);
            }

            // Simulation
            // Adding the reward of the whole rollout is not 100% accurate, but the best we can do
            rewardAccumulator.addReward(rollout(game, depth, rewardAccumulator));
        }

        // Backpropagation
        currNode.backpropagate(rewardAccumulator.getRewardSum());
        rewardAccumulator.reset();
    }

    public int getNextAction()
    {
        if(root.children.size() == 0)
        {
            return -1;
        }

        if(root.children.size() > 1)
        {
            // Only keep the best child
            MCTSNode<SubgoalData> selectedChild = getBestChild();
            root.children.clear();
            root.children.add(selectedChild);
        }

        BaseAction nextAction = root.children.get(0).data.macroAction.actions.get(0);
        nextAction.repetitions--;
        if(nextAction.repetitions == 0)
        {
            root.children.get(0).data.macroAction.actions.remove(0);
            // Only one action left, aka this is our new root
            if(root.children.get(0).data.macroAction.actions.size() == 0)
            {
                root = root.children.get(0);
            }
        }

        return nextAction.lowLevelAction;
    }

    public MCTSNode<SubgoalData> getBestChild()
    {
        double maxScore = Double.NEGATIVE_INFINITY;
        MCTSNode<SubgoalData> bestChild = null;
        for(MCTSNode<SubgoalData> child : root.children)
        {
            if(child.score > maxScore)
            {
                maxScore = child.score;
                bestChild = child;
            }
        }

        return bestChild;
    }

    public MCTSNode<SubgoalData> getRoot()
    {
        return root;
    }

    private double rollout(RewardGame state, int currentDepth, RewardAccumulator accumulator)
    {
        double rewardSum = 0;
        while(!state.isEnded() && currentDepth <= maxRolloutDepth)
        {
            BaseAction nextAction = new BaseAction(rng.nextInt(Controller.NUM_ACTIONS));
            rewardSum += nextAction.apply(state);

            currentDepth++;
        }
        return rewardSum;
    }
}
