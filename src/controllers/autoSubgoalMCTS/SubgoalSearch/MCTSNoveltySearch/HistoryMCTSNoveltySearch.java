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

public class HistoryMCTSNoveltySearch implements ISubgoalSearch
{
    public HistoryMCTSNoveltySearch(int trajectoryLength, IBehaviourFunction behaviourFunction, Random rng)
    {
        this.trajectoryLength = trajectoryLength;
        this.behaviourFunction = behaviourFunction;
        this.rng = rng;

        this.root = new MCTSNode<>(new SearchData());
        this.noveltyAccumulator = new RewardAccumulator(0.99);
        this.rewardAccumulator = new RewardAccumulator(0.99);
        this.latentCache = new double[behaviourFunction.getLatentSize()];
        this.rootCache = new double[behaviourFunction.getLatentSize()];
    }

    public ISubgoalSearch createNewSearch(MCTSNode<SubgoalData> parentNode)
    {
        HistoryMCTSNoveltySearch newSearch = new HistoryMCTSNoveltySearch(trajectoryLength, behaviourFunction, rng);
        newSearch.parentNode = parentNode;
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
            currNode = currNode.selectUCT(Math.sqrt(2), rng);
            advanceGame(game, currNode.data.action);
            depth++;
        }

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
        return root.visitCount >= 400;
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
        ArrayList<MCTSNode<SearchData>> selectedNodes = new ArrayList<>();
        ArrayList<Subgoal> archive = new ArrayList<>();

        // Add history of subgoals to the archive
        MCTSNode<SubgoalData> currNode = parentNode;
        while(!currNode.isRootNode())
        {
            Subgoal s = new Subgoal();
            s.latentState = currNode.data.latentState;
            s.reward = currNode.score;
            archive.add(s);
            currNode = currNode.parent;
        }

        // If we have no yet, then use the one with the highest reward
        if(archive.size() == 0)
        {
            MCTSNode<SearchData> bestSubgoal = subgoalCandidates.stream().max(Comparator.comparing(n -> n.data.reward)).get();
            selectedNodes.add(bestSubgoal);
            Subgoal s = new Subgoal();
            s.latentState = bestSubgoal.data.latentState;
            s.reward = bestSubgoal.data.reward;
            archive.add(s);
        }

        double[] noveltyCache = new double[subgoalCandidates.size()];
        double[] rewardCache = new double[subgoalCandidates.size()];
        double minNovelty = Double.POSITIVE_INFINITY;
        double maxNovelty = Double.NEGATIVE_INFINITY;
        while(selectedNodes.size() < subgoalCount)
        {
            // Compute the novelty & reward for each candidate
            for(int i = 0; i < subgoalCandidates.size(); i++)
            {
                MCTSNode<SearchData> candidate = subgoalCandidates.get(i);

                // Find the n-closest neighbors from all selected subgoals
                int n = archive.size() > 3 ? 3 : archive.size();
                Collections.sort(archive, (Subgoal v1, Subgoal v2) ->
                {
                    double v1Dist = latentDist(v1.latentState, candidate.data.latentState);
                    double v2Dist = latentDist(v2.latentState, candidate.data.latentState);
                    return v1Dist > v2Dist ? 1 : v1Dist < v2Dist ? -1 : 0;
                });

                // Compute novelty & rewards scores
                noveltyCache[i] = 0;
                rewardCache[i] = 3;
                for(int x = 0; x < n; x++)
                {
                    noveltyCache[i] += latentDist(candidate.data.latentState, archive.get(x).latentState);
                    if(archive.get(x).reward >= candidate.data.reward)
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

            selectedNodes.add(bestCandidate);
            Subgoal s = new Subgoal();
            s.latentState = bestCandidate.data.latentState;
            s.reward = bestCandidate.data.reward;
            archive.add(s);
        }

        // Convert to macro actions
        ArrayList<MacroAction> macroActions = new ArrayList<>();
        for(MCTSNode<SearchData> subgoal : selectedNodes)
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

    private MCTSNode<SubgoalData> parentNode;
    private MCTSNode<SearchData> root;
    private int trajectoryLength;
    private RewardAccumulator noveltyAccumulator;
    private RewardAccumulator rewardAccumulator;
    public IBehaviourFunction behaviourFunction;
    private double[] latentCache;
    private double[] rootCache;
    private Random rng;
}
