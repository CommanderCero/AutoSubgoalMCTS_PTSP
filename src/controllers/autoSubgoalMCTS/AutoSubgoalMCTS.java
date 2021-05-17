package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import framework.core.Controller;
import framework.core.Game;

import java.util.ArrayList;
import java.util.Collections;

public class AutoSubgoalMCTS
{
    private RewardGame initialGame;
    private MCTSNode<SubgoalData> root;
    private ISubgoalSearch subgoalSearch;
    private int n;

    private RewardAccumulator rewardAccumulator;
    private RewardAccumulator macroAccumulator;

    public double explorationRate = Math.sqrt(2);
    public int maxSimulationSteps = 100;

    public AutoSubgoalMCTS(RewardGame initialGame, ISubgoalSearch subgoalSearch, int n)
    {
        this.initialGame = initialGame.getCopy();
        this.root = new MCTSNode<>(new SubgoalData());
        this.root.data.subgoalSearch = subgoalSearch.copy();
        this.subgoalSearch = subgoalSearch;
        this.n = n;

        rewardAccumulator = new RewardAccumulator(0.99);
        macroAccumulator = new RewardAccumulator(0.99);
    }

    public void setInitialState(Game newInitialState)
    {
        initialGame.setState(newInitialState);
    }

    public void step()
    {
        // Selection
        RewardGame game = initialGame.getCopy();
        MCTSNode<SubgoalData> currNode = root;
        while (!game.isEnded() && currNode.data.subgoalSearch == null)
        {
            currNode = currNode.selectUCT(explorationRate);
            for(BaseAction action : currNode.data.macroAction)
            {
                rewardAccumulator.addReward(action.apply(game));
            }
        }

        if (!game.isEnded())
        {
            // Expansion
            if(currNode.data.subgoalSearch.isDone())
            {
                currNode.data.subgoalSearch.addSubgoals(currNode);
                for(MCTSNode<SubgoalData> child : currNode.children)
                {
                    child.data.subgoalSearch = subgoalSearch.copy();
                }
                currNode.data.subgoalSearch = null;
            }
            else
            {
                currNode.data.subgoalSearch.step(game);
            }

            // Simulation
            // simulate(state, rngEngine);
        }

        // Backpropagation
        currNode.backpropagate(rewardAccumulator.getRewardSum());
        rewardAccumulator.reset();
    }

    public int getNextAction()
    {
        if(root.children.size() == 0)
        {
            System.out.println("Error");
            return 0;
        }

        if(root.children.size() > 1)
        {
            // Only keep the best child
            MCTSNode<SubgoalData> selectedChild = getBestChild();
            root.children.clear();
            root.children.add(selectedChild);
        }

        BaseAction nextAction = root.children.get(0).data.macroAction.get(0);
        nextAction.repetitions--;
        if(nextAction.repetitions == 0)
        {
            root.children.get(0).data.macroAction.remove(0);
            // Only one action left, aka this is our new root
            if(root.children.get(0).data.macroAction.size() == 0)
            {
                root = root.children.get(0);
            }
        }

        return nextAction.lowLevelAction;
    }

    private void updateStatistics(MCTSNode<SubgoalData> currNode, double receivedReward)
    {
        //currNode.score -= (currNode.children.size() * receivedReward) / currNode.visitCount;
        //currNode.score /= rewardAccumulator.rewardDecay;

        for(MCTSNode<SubgoalData> child : currNode.children)
        {
            updateStatistics(child, receivedReward);
        }
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
}
