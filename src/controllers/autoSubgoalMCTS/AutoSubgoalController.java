package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.BehaviourFunctions.IBehaviourFunction;
import controllers.autoSubgoalMCTS.BehaviourFunctions.PositionBehaviourFunction;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalPredicates.PositionGridPredicate;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.MCTSNoveltySearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.RandomPredicateSearch.RandomPredicateSearch;
import framework.core.Controller;
import framework.core.Game;

import java.awt.*;

public class AutoSubgoalController extends AbstractController
{
    // Ugly hack
    public static ISubgoalSearch subgoalSearch;

    public int maxRolloutDepth = 25;
    public double explorationRate = Math.sqrt(2);

    private MCTSNode<SubgoalData> root;
    private RewardAccumulator rewardAccumulator;

    public AutoSubgoalController(Game game, long dueTimeMs)
    {
        // Failsafe
        if(subgoalSearch == null)
        {
            //PositionGridPredicate predicate = new PositionGridPredicate(20, 3);
            //RandomPredicateSearch.treatHorizonStatesAsSubgoals = false;
            //subgoalSearch = new RandomPredicateSearch(predicate, 4, 400, rng);
            subgoalSearch = new MCTSNoveltySearch(4, new PositionBehaviourFunction(), rng);
        }

        this.root = new MCTSNode<>(new SubgoalData());
        this.root.data.subgoalSearch = subgoalSearch.copy();

        rewardAccumulator = new RewardAccumulator(0.99);
    }

    @Override
    protected void step(RewardGame game)
    {
        // Selection
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

    @Override
    protected BaseAction getBestAction()
    {
        if(root.children.size() == 0)
        {
            System.out.println("Warning no subgoals found in time");
            return new BaseAction(rng.nextInt(NUM_ACTIONS));
        }

        if(root.children.size() > 1)
        {
            // Only keep the best child
            MCTSNode<SubgoalData> selectedChild = getBestChild();
            root.children.clear();
            root.children.add(selectedChild);
        }

        BaseAction nextAction = root.children.get(0).data.macroAction.actions.get(0);
        root.children.get(0).data.macroAction.actions.remove(0);
        // Only one action left, aka this is our new root
        if(root.children.get(0).data.macroAction.size() == 0)
        {
            root = root.children.get(0);
            root.parent = null;
        }

        return nextAction;
    }

    private MCTSNode<SubgoalData> getBestChild()
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

    @Override
    public synchronized void paint(Graphics2D graphics)
    {
        graphics.setColor(Color.yellow);
        drawSubgoals(graphics, root);
        //PositionGridPredicate predicate = new PositionGridPredicate(25, 5);
        //predicate.render(graphics, lastState);
    }

    private void drawSubgoals(Graphics2D graphics, MCTSNode<SubgoalData> node)
    {
        if(node.data.lastSeenPosition == null)
            return;

        int r = 2;
        graphics.fillOval((int)node.data.lastSeenPosition.x - r, (int)node.data.lastSeenPosition.y - r, 2 * r, 2 * r);

        for(int i = 0; i < node.children.size(); i++)
        {
            MCTSNode<SubgoalData> child = node.children.get(i);
            if(child.data.lastSeenPosition == null)
                continue;


            graphics.drawLine((int)node.data.lastSeenPosition.x, (int)node.data.lastSeenPosition.y, (int)child.data.lastSeenPosition.x, (int)child.data.lastSeenPosition.y);
            drawSubgoals(graphics, child);
        }
    }
}
