package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.BehaviourFunctions.IBehaviourFunction;
import controllers.autoSubgoalMCTS.BehaviourFunctions.PositionBehaviourFunction;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalPredicates.PositionGridPredicate;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.HistoryMCTSNoveltySearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.MCTSNoveltySearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.SearchData;
import controllers.autoSubgoalMCTS.SubgoalSearch.RandomPredicateSearch.RandomPredicateSearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.ScalarNSLCSearch.ScalarNSLCSearch;
import framework.core.Controller;
import framework.core.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class AutoSubgoalController extends AbstractController
{
    // Ugly hack
    public static ISubgoalSearch subgoalSearch;
    public static double explorationRate = 4;

    public static int maxRolloutDepth = 25;

    private MCTSNode<SubgoalData> root;
    private RewardAccumulator rewardAccumulator;
    private IBehaviourFunction behaviourFunction;

    public AutoSubgoalController(Game game, long dueTimeMs)
    {
        // Failsafe
        behaviourFunction = new PositionBehaviourFunction();
        if(subgoalSearch == null)
        {
            //PositionGridPredicate predicate = new PositionGridPredicate(35, 3);
            //RandomPredicateSearch.treatHorizonStatesAsSubgoals = false;
            //subgoalSearch = new RandomPredicateSearch(predicate, 4, 400, rng);
            subgoalSearch = new MCTSNoveltySearch(4, behaviourFunction, rng);
            MCTSNoveltySearch.explorationRate = Math.sqrt(2);
            MCTSNoveltySearch.maxSteps = 400;
            //subgoalSearch = new HistoryMCTSNoveltySearch(4, behaviourFunction, rng);
            //subgoalSearch = new ScalarNSLCSearch(behaviourFunction, rng, 200, 3);
        }

        this.root = new MCTSNode<>(new SubgoalData());
        this.root.data.latentState = new double[behaviourFunction.getLatentSize()];
        this.root.data.subgoalSearch = subgoalSearch.createNewSearch(this.root);

        rewardAccumulator = new RewardAccumulator(1);
    }

    @Override
    protected void step(RewardGame game)
    {
        // Selection
        ArrayList<MCTSNode<SubgoalData>> subgoalHistory = new ArrayList<>();
        MCTSNode<SubgoalData> currNode = root;
        int depth = 0;
        while (!game.isEnded() && currNode.data.subgoalSearch == null && depth < maxRolloutDepth)
        {
            subgoalHistory.add(currNode);
            currNode.data.lastSeenPosition = game.getState().getShip().s.copy();
            behaviourFunction.toLatent(game.getState(), currNode.data.latentState);

            currNode = selectUCT(currNode, explorationRate, rng);
            rewardAccumulator.addReward(currNode.data.macroAction.apply(game));
            depth += currNode.data.macroAction.size();
        }
        subgoalHistory.add(currNode);
        currNode.data.lastSeenPosition = game.getState().getShip().s.copy();
        behaviourFunction.toLatent(game.getState(), currNode.data.latentState);

        // Expansion
        if (!game.isEnded() && depth < maxRolloutDepth)
        {
            if(currNode.data.subgoalSearch.isDone())
            {
                for(MacroAction a : currNode.data.subgoalSearch.getMacroActions())
                {
                    SubgoalData newData = new SubgoalData();
                    MCTSNode<SubgoalData> newNode = currNode.addChild(newData);
                    newData.subgoalSearch = subgoalSearch.createNewSearch(newNode);
                    newData.macroAction = a;
                    newData.latentState = new double[behaviourFunction.getLatentSize()];
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
            rewardAccumulator.addReward(rollout(game, depth, rewardAccumulator));
        }

        // Backpropagation
        double noveltyScore = 0;
        double[] currLatentPos = new double[behaviourFunction.getLatentSize()];
        behaviourFunction.toLatent(game.getState(), currLatentPos);
        // Find the n-closest neighbors from all subgoals
        int n = subgoalHistory.size() > 3 ? 3 : subgoalHistory.size();
        Collections.sort(subgoalHistory, (MCTSNode<SubgoalData> v1, MCTSNode<SubgoalData> v2) ->
        {
            double v1Dist = latentDist(v1.data.latentState, currLatentPos);
            double v2Dist = latentDist(v2.data.latentState, currLatentPos);
            return v1Dist > v2Dist ? 1 : v1Dist < v2Dist ? -1 : 0;
        });

        for(int i = 0; i < n; i++)
        {
            noveltyScore += latentDist(currLatentPos, subgoalHistory.get(i).data.latentState);
        }
        noveltyScore /= n;

        double test = noveltyScore;
        currNode.backpropagate(rewardAccumulator.getRewardSum(), node ->
        {
            node.data.noveltyScore += (test - node.data.noveltyScore) / node.visitCount;
            node.data.noveltyLowerBound = Math.min(test, node.data.noveltyLowerBound);
            node.data.noveltyUpperBound = Math.max(test, node.data.noveltyUpperBound);
        });
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

    private MCTSNode<SubgoalData> selectUCT(MCTSNode<SubgoalData> node, double explorationRate, Random rng)
    {
        double highestUCT = Double.NEGATIVE_INFINITY;
        MCTSNode<SubgoalData> bestChild = null;
        for (MCTSNode<SubgoalData> child : node.children)
        {
            if (child.fullyExplored)
                continue;
            if (child.visitCount == 0)
                return child;

            double uct = (child.score - node.lowerBound) / (node.upperBound - node.lowerBound + 1);
            uct += explorationRate * Math.sqrt(Math.log(node.visitCount) / child.visitCount);
            uct += rng.nextDouble() * 1e-8; // Resolve ties randomly

            if (uct > highestUCT)
            {
                highestUCT = uct;
                bestChild = child;
            }
        }

        return bestChild;
    }

    private double latentDist(double[] v1, double[] v2)
    {
        double sumSquared = 0;
        for(int i = 0; i < v1.length; i++)
        {
            double delta = v1[i] - v2[i];
            sumSquared += delta * delta;
        }
        return Math.sqrt(sumSquared);
    }
}
