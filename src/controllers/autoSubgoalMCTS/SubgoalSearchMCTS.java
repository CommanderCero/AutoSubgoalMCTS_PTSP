package controllers.autoSubgoalMCTS;

import controllers.mcts.MacroAction;
import framework.core.Controller;
import framework.core.Game;

import java.util.ArrayList;
import java.util.Collections;

public class SubgoalSearchMCTS
{
    public SubgoalSearchMCTS(int trajectoryLength, IBehaviourFunction behaviourFunction)
    {
        this.trajectoryLength = trajectoryLength;
        this.behaviourFunction = behaviourFunction;
        this.root = new MCTSNode<>(new SearchData());
        this.macroAccumulator = new RewardAccumulator(0.99);
        this.latentCache = new double[behaviourFunction.getLatentSize()];
        this.rootCache = new double[behaviourFunction.getLatentSize()];
    }

    public SubgoalSearchMCTS initNewSearch()
    {
        SubgoalSearchMCTS newSearch = new SubgoalSearchMCTS(trajectoryLength, behaviourFunction);
        return newSearch;
    }

    public void step(Game state)
    {
        behaviourFunction.toLatent(state, rootCache);

        // Selection
        MCTSNode<SearchData> currNode = root;
        while(currNode.children.size() == Controller.NUM_ACTIONS)
        {
            currNode = currNode.selectUCT(Math.sqrt(2));
            advanceState(state, currNode.data.action);
        }

        // Expansion
        BaseAction nextAction = new BaseAction(currNode.children.size());
        advanceState(state, nextAction);

        SearchData macroData = new SearchData();
        macroData.action = nextAction;
        macroData.latentState = new double[behaviourFunction.getLatentSize()];
        behaviourFunction.toLatent(state, macroData.latentState);
        currNode = currNode.addChild(macroData);

        // Backpropagation
        currNode.backpropagate(macroAccumulator.getRewardSum());
        macroAccumulator.reset();
    }

    public void addSubgoals(MCTSNode<SubgoalData> node)
    {
        ArrayList<MCTSNode<SearchData>> subgoalCandidates = new ArrayList<>();
        selectSubgoalCandidates(root, 0, subgoalCandidates);

        double percentage = 0.02;
        int subgoalCount = (int)Math.ceil(percentage * subgoalCandidates.size());
        assert(subgoalCount > 0);
        while(node.children.size() < subgoalCount)
        {
            // Find best candidate
            MCTSNode<SearchData> bestCandidate = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for(MCTSNode<SearchData> candidate : subgoalCandidates)
            {
                double score = 0;
                for(int i = 0; i < node.children.size(); i++)
                {
                    score += latentDist(candidate.data.latentState, node.children.get(i).data.latentState);
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
            node.addChild(subgoalData);
        }
    }

    private void selectSubgoalCandidates(MCTSNode<SearchData> node, int count, ArrayList<MCTSNode<SearchData>> bucket)
    {
        if(count == trajectoryLength)
        {
            if(node.visitCount > 0)
                bucket.add(node);
            return;
        }

        count++;
        for(MCTSNode<SearchData> child : node.children)
        {
            selectSubgoalCandidates(child, count, bucket);
        }
    }

    private void advanceState(Game state, BaseAction action)
    {
        behaviourFunction.toLatent(state, latentCache);
        double distanceBefore = latentDist(rootCache, latentCache);
        // ToDo return reward
        action.apply(state);
        behaviourFunction.toLatent(state, latentCache);
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

    private class SearchData
    {
        public BaseAction action;
        double reward;
        double[] latentState;
    }

    private MCTSNode<SearchData> root;
    private int trajectoryLength;
    private RewardAccumulator macroAccumulator;
    public IBehaviourFunction behaviourFunction;
    private double[] latentCache;
    private double[] rootCache;
}
