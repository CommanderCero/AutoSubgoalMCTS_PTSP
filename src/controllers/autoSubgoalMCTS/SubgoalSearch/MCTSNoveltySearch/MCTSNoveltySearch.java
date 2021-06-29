package controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch;

import controllers.autoSubgoalMCTS.*;
import controllers.autoSubgoalMCTS.BehaviourFunctions.IBehaviourFunction;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import framework.core.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class MCTSNoveltySearch implements ISubgoalSearch
{
    public static double explorationRate = Math.sqrt(2);
    public static int maxSteps = 400;

    public MCTSNoveltySearch(int trajectoryLength, IBehaviourFunction behaviourFunction, Random rng)
    {
        this.trajectoryLength = trajectoryLength;
        this.behaviourFunction = behaviourFunction;
        this.rng = rng;

        this.root = new MCTSNode<>(new SearchData());
        this.root.data.latentState = new double[behaviourFunction.getLatentSize()];
        this.noveltyAccumulator = new RewardAccumulator(0.99);
        this.rewardAccumulator = new RewardAccumulator(0.99);
        this.latentCache = new double[behaviourFunction.getLatentSize()];
        this.rootCache = new double[behaviourFunction.getLatentSize()];
    }

    public ISubgoalSearch createNewSearch(MCTSNode<SubgoalData> parentNode)
    {
        MCTSNoveltySearch newSearch = new MCTSNoveltySearch(trajectoryLength, behaviourFunction, rng);
        return newSearch;
    }

    @Override
    public int step(RewardGame game)
    {
        behaviourFunction.toLatent(game.getState(), rootCache);

        // Selection
        int depth = 0;
        MCTSNode<SearchData> currNode = root;
        while(currNode.children.size() == Controller.NUM_ACTIONS && depth < trajectoryLength)
        {
            behaviourFunction.toLatent(game.getState(), currNode.data.latentState);
            currNode = currNode.selectUCT(explorationRate, rng);
            advanceGame(game, currNode.data.action);
            depth++;
        }
        behaviourFunction.toLatent(game.getState(), currNode.data.latentState);

        // Expansion
        if(depth < trajectoryLength)
        {
            BaseAction nextAction = new BaseAction(currNode.children.size());
            advanceGame(game, nextAction);
            depth++;

            SearchData macroData = new SearchData();
            macroData.action = nextAction;
            macroData.latentState = new double[behaviourFunction.getLatentSize()];
            behaviourFunction.toLatent(game.getState(), macroData.latentState);
            currNode = currNode.addChild(macroData);
        }

        // Backpropagation
        currNode.backpropagate(noveltyAccumulator.getRewardSum(), (MCTSNode<SearchData> n) ->
        {
            // Dirty hack to track two different statistics, in this case novelty & reward
            n.data.reward += (rewardAccumulator.getRewardSum() - n.data.reward) / n.visitCount;
            n.data.rewardLowerBound = Math.min(rewardAccumulator.getRewardSum(), n.data.rewardLowerBound);
            n.data.rewardUpperBound = Math.max(rewardAccumulator.getRewardSum(), n.data.rewardUpperBound);
        });
        noveltyAccumulator.reset();
        rewardAccumulator.reset();
        return depth;
    }

    @Override
    public boolean isDone()
    {
        return root.visitCount >= maxSteps;
    }

    @Override
    public ArrayList<MacroAction> getMacroActions()
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
        ArrayList<MCTSNode<SearchData>> selectedSubgoals = new ArrayList<>();
        // The first subgoal is the one with the highest reward
        selectedSubgoals.add(subgoalCandidates.stream().max(Comparator.comparing(n -> n.data.reward)).get());

        double[] noveltyCache = new double[subgoalCandidates.size()];
        double[] rewardCache = new double[subgoalCandidates.size()];
        double minNovelty = Double.POSITIVE_INFINITY;
        double maxNovelty = Double.NEGATIVE_INFINITY;
        while(selectedSubgoals.size() < subgoalCount)
        {
            // Compute the novelty & reward for each candidate
            for(int i = 0; i < subgoalCandidates.size(); i++)
            {
                MCTSNode<SearchData> candidate = subgoalCandidates.get(i);

                // Find the n-closest neighbors from all selected subgoals
                int n = selectedSubgoals.size() > 3 ? 3 : selectedSubgoals.size();
                Collections.sort(selectedSubgoals, (MCTSNode<SearchData> v1, MCTSNode<SearchData> v2) ->
                {
                    double v1Dist = latentDist(v1.data.latentState, candidate.data.latentState);
                    double v2Dist = latentDist(v2.data.latentState, candidate.data.latentState);
                    return v1Dist > v2Dist ? 1 : v1Dist < v2Dist ? -1 : 0;
                });

                // Compute novelty & rewards scores
                noveltyCache[i] = 0;
                rewardCache[i] = 3;
                for(int x = 0; x < n; x++)
                {
                    noveltyCache[i] += latentDist(candidate.data.latentState, selectedSubgoals.get(x).data.latentState);
                    if(selectedSubgoals.get(x).data.reward >= candidate.data.reward)
                    {
                        rewardCache[i]--;
                    }
                }
                minNovelty = Math.min(minNovelty, noveltyCache[i]);
                maxNovelty = Math.max(maxNovelty, noveltyCache[i]);
            }

            // Find best candidate
            MCTSNode<SearchData> bestCandidate = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for(int i = 0; i < subgoalCandidates.size(); i++)
            {
                double score = (noveltyCache[i] - minNovelty) / (maxNovelty - minNovelty + 1e-8);
                score = 0.5 * score + 0.5 * (rewardCache[i] / 3);

                if(score > bestScore)
                {
                    bestCandidate = subgoalCandidates.get(i);
                    bestScore = score;
                }
            }

            selectedSubgoals.add(bestCandidate);
        }

        // Convert to macro actions
        ArrayList<MacroAction> macroActions = new ArrayList<>();
        for(MCTSNode<SearchData> subgoal : selectedSubgoals)
        {
            // Collect macro action
            MacroAction newMacroAction = new MacroAction();
            MCTSNode<SearchData> tmpNode = subgoal;
            while (tmpNode.parent != null)
            {
                newMacroAction.actions.add(tmpNode.data.action);
                tmpNode = tmpNode.parent;
            }
            // We collect the actions from bottom to top, meaning they are in the wrong order
            Collections.reverse(newMacroAction.actions);
            macroActions.add(newMacroAction);
        }

        return macroActions;
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
        rewardAccumulator.addReward(action.apply(game));
        behaviourFunction.toLatent(game.getState(), latentCache);
        double distanceAfter = latentDist(rootCache, latentCache);
        noveltyAccumulator.addReward(distanceAfter - distanceBefore);
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
    private RewardAccumulator noveltyAccumulator;
    private RewardAccumulator rewardAccumulator;
    public IBehaviourFunction behaviourFunction;
    private double[] latentCache;
    private double[] rootCache;
    private Random rng;
}
