package controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch;

import controllers.autoSubgoalMCTS.*;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import framework.core.Controller;

import java.util.ArrayList;
import java.util.Collections;

public class MCTSNoveltySearch implements ISubgoalSearch
{
    public MCTSNoveltySearch(int trajectoryLength, IBehaviourFunction behaviourFunction)
    {
        this.trajectoryLength = trajectoryLength;
        this.behaviourFunction = behaviourFunction;
        this.root = new MCTSNode<>(new SearchData());
        this.macroAccumulator = new RewardAccumulator(0.99);
        this.latentCache = new double[behaviourFunction.getLatentSize()];
        this.rootCache = new double[behaviourFunction.getLatentSize()];
    }

    public ISubgoalSearch copy()
    {
        MCTSNoveltySearch newSearch = new MCTSNoveltySearch(trajectoryLength, behaviourFunction);
        return newSearch;
    }

    @Override
    public void step(RewardGame game)
    {
        behaviourFunction.toLatent(game.getState(), rootCache);

        // Selection
        MCTSNode<SearchData> currNode = root;
        while(currNode.children.size() == Controller.NUM_ACTIONS)
        {
            currNode = currNode.selectUCT(Math.sqrt(2));
            advanceGame(game, currNode.data.action);
        }

        // Expansion
        BaseAction nextAction = new BaseAction(currNode.children.size());
        advanceGame(game, nextAction);

        SearchData macroData = new SearchData();
        macroData.action = nextAction;
        macroData.latentState = new double[behaviourFunction.getLatentSize()];
        behaviourFunction.toLatent(game.getState(), macroData.latentState);
        currNode = currNode.addChild(macroData);

        // Backpropagation
        currNode.backpropagate(macroAccumulator.getRewardSum());
        macroAccumulator.reset();
    }

    @Override
    public boolean isDone()
    {
        return root.visitCount >= 400;
    }

    @Override
    public void addSubgoals(MCTSNode<SubgoalData> parentNode)
    {
        ArrayList<MCTSNode<SearchData>> subgoalCandidates = new ArrayList<>();
        int tmpTrajectoryLength = trajectoryLength;
        // Keep reducing the trajectory length if we didnt find any suitable candidates
        while(subgoalCandidates.size() == 0)
        {
            assert(tmpTrajectoryLength > 0);
            selectSubgoalCandidates(root, tmpTrajectoryLength, subgoalCandidates);
            tmpTrajectoryLength--;
        }

        double percentage = 0.02;
        int subgoalCount = (int)Math.ceil(percentage * subgoalCandidates.size());
        assert(subgoalCount > 0);
        while(parentNode.children.size() < subgoalCount)
        {
            // Find best candidate
            MCTSNode<SearchData> bestCandidate = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for(MCTSNode<SearchData> candidate : subgoalCandidates)
            {
                double score = 0;
                for(int i = 0; i < parentNode.children.size(); i++)
                {
                    score += latentDist(candidate.data.latentState, parentNode.children.get(i).data.latentState);
                }
                if(score > bestScore)
                {
                    bestCandidate = candidate;
                    bestScore = score;
                }
            }

            // Construct subgoal
            SubgoalData subgoalData = new SubgoalData();
            subgoalData.latentState = bestCandidate.data.latentState;
            // Collect macro action
            MCTSNode<SearchData> tmpNode = bestCandidate;
            while (tmpNode.parent != null)
            {
                subgoalData.macroAction.add(tmpNode.data.action);
                tmpNode = tmpNode.parent;
            }
            // We collect the actions from bottom to top, meaning they are in the wrong order
            Collections.reverse(subgoalData.macroAction);
            parentNode.addChild(subgoalData);
        }
    }

    private void selectSubgoalCandidates(MCTSNode<SearchData> node, int count, ArrayList<MCTSNode<SearchData>> bucket)
    {
        if(count == 0)
        {
            if(node.visitCount > 0)
                bucket.add(node);
            return;
        }

        count--;
        for(MCTSNode<SearchData> child : node.children)
        {
            selectSubgoalCandidates(child, count, bucket);
        }
    }

    private void advanceGame(RewardGame game, BaseAction action)
    {
        behaviourFunction.toLatent(game.getState(), latentCache);
        double distanceBefore = latentDist(rootCache, latentCache);
        // ToDo return reward
        action.apply(game);
        behaviourFunction.toLatent(game.getState(), latentCache);
        double distanceAfter = latentDist(rootCache, latentCache);
        macroAccumulator.addReward(distanceAfter - distanceBefore);
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

    private MCTSNode<SearchData> root;
    private int trajectoryLength;
    private RewardAccumulator macroAccumulator;
    public IBehaviourFunction behaviourFunction;
    private double[] latentCache;
    private double[] rootCache;
}
