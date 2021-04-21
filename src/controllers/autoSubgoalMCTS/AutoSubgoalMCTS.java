package controllers.autoSubgoalMCTS;

import framework.core.Controller;
import framework.core.Game;

import java.util.ArrayList;
import java.util.Collections;

public class AutoSubgoalMCTS
{
    double latentDist(double[] l1, double[] l2)
    {
        double sumSquared = 0;
        for(int i = 0; i < l1.length; i++)
        {
            double delta = l1[i] - l2[i];
            sumSquared += delta * delta;
        }
        return Math.sqrt(sumSquared);
    }

    public class MacroData
    {
        public int action;
        double reward;
        double[] latentState;
    };

    public class SubgoalData
    {
        public SubgoalData()
        {
            macroAction = new ArrayList<>();
        }

        double[] latentState;
        ArrayList<Integer> macroAction;
        MCTSNode<MacroData> macroRoot;
    };

    private Game initialState;
    private MCTSNode<SubgoalData> root;
    private IBehaviourFunction distanceMetric;
    private int k;
    private int n;
    private double[] latentCache;
    private ArrayList<MCTSNode<SubgoalData>> discoveredSubgoals;

    private RewardAccumulator rewardAccumulator;
    private RewardAccumulator macroAccumulator;

    public double explorationRate = Math.sqrt(2);
    public int maxSimulationSteps = 100;

    public AutoSubgoalMCTS(Game initialState, IBehaviourFunction distanceMetric, int k, int n)
    {
        this.initialState = initialState.getCopy();
        this.root = new MCTSNode<>(new SubgoalData());
        this.root.data.macroRoot = new MCTSNode<>(new MacroData());
        this.distanceMetric = distanceMetric;
        this.k = k;
        this.n = n;
        discoveredSubgoals = new ArrayList<>();

        rewardAccumulator = new RewardAccumulator(1);
        macroAccumulator = new RewardAccumulator(0.99);

        this.latentCache = new double[distanceMetric.getLatentSize()];
        this.root.data.latentState = new double[distanceMetric.getLatentSize()];
        distanceMetric.toLatent(initialState, this.root.data.latentState);
        discoveredSubgoals.add(this.root);
    }

    public void step()
    {
        if (root.fullyExplored)
        {
            return;
        }

        // Selection
        Game state = initialState.getCopy();
        MCTSNode<SubgoalData> currNode = root;
        while (!currNode.isLeafNode())
        {
            MCTSNode<SubgoalData> lastNode = currNode;
            currNode = currNode.selectUCT(explorationRate);
            for(int action : currNode.data.macroAction)
            {
                advanceState(state, action);
            }
        }

        if (!state.isEnded())
        {
            // Expansion
            extendNode(currNode, state);

            // Simulation
            // simulate(state, rngEngine);
        }

        // Backpropagation
        currNode.backpropagate(rewardAccumulator.getRewardSum());
        rewardAccumulator.reset();
    }

    public void advanceTree(MCTSNode<SubgoalData> selectedChild)
    {
        for(int action : selectedChild.data.macroAction)
        {
            advanceState(initialState, action);
        }

        root = selectedChild;
        updateStatistics(selectedChild, rewardAccumulator.getRewardSum());
        rewardAccumulator.reset();

        discoveredSubgoals.clear();
        resetSubgoals(root);
    }

    private void resetSubgoals(MCTSNode<SubgoalData> newRoot)
    {
        discoveredSubgoals.add(newRoot);
        for(MCTSNode<SubgoalData> child : newRoot.children)
        {
            resetSubgoals(child);
        }
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

    private void extendNode(MCTSNode<SubgoalData> node, Game state)
    {
        if(node.visitCount > n)
        {
            selectSubgoals(node);
            node.data.macroRoot = null;
        }
        else
        {
            // Selection
            MCTSNode<MacroData> currNode = node.data.macroRoot;
            while(currNode.children.size() == Controller.NUM_ACTIONS)
            {
                currNode = currNode.selectUCT(Math.sqrt(2));
                advanceStateMacro(node, state, currNode.data.action);
            }

            // Expansion
            int nextAction = currNode.children.size();
            advanceStateMacro(node, state, nextAction);

            MacroData macroData = new MacroData();
            macroData.action = nextAction;
            macroData.latentState = new double[distanceMetric.getLatentSize()];
            distanceMetric.toLatent(state, macroData.latentState);
            currNode = currNode.addChild(macroData);

            // Backpropagation
            currNode.backpropagate(macroAccumulator.getRewardSum(), backpropNode ->
            {
                backpropNode.data.reward += (rewardAccumulator.getRewardSum() - backpropNode.data.reward) / backpropNode.visitCount;
            });
            macroAccumulator.reset();
        }
    }

    private void selectSubgoals(MCTSNode<SubgoalData> node)
    {
        ArrayList<MCTSNode<MacroData>> subgoalCandidates = new ArrayList<>();
        selectSubgoalCandidates(node.data.macroRoot, 0, subgoalCandidates);

        double percentage = 0.02;
        int subgoalCount = (int)Math.ceil(percentage * subgoalCandidates.size());
        assert(subgoalCount > 0);
        while(node.children.size() < subgoalCount)
        {
            // Find best candidate
            MCTSNode<MacroData> bestCandidate = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for(MCTSNode<MacroData> candidate : subgoalCandidates)
            {
                // Compute n closest subgoals
                int n = discoveredSubgoals.size() > 3 ? 3 : discoveredSubgoals.size();
                Collections.sort(discoveredSubgoals, (MCTSNode<SubgoalData> v1, MCTSNode<SubgoalData> v2) ->
                {
                    double v2Dist = latentDist(v2.data.latentState, candidate.data.latentState);
                    double v1Dist = latentDist(v1.data.latentState, candidate.data.latentState);
                    return v1Dist > v2Dist ? 1 : v1Dist < v2Dist ? -1 : 0;
                });

                double score = 0;
                for(int i = 0; i < n; i++)
                {
                    score += latentDist(candidate.data.latentState, discoveredSubgoals.get(i).data.latentState);
                }
                if(score > bestScore)
                {
                    bestCandidate = candidate;
                    bestScore = score;
                }
            }

            // Generate subgoal data
            SubgoalData data = new SubgoalData();
            data.macroRoot = new MCTSNode<>(new MacroData());
            data.latentState = bestCandidate.data.latentState;
            // Collect macro action
            data.macroAction = new ArrayList<>();
            MCTSNode<MacroData> tmpNode = bestCandidate;
            while (tmpNode.parent != null)
            {
                data.macroAction.add(tmpNode.data.action);
                tmpNode = tmpNode.parent;
            }
            Collections.reverse(data.macroAction);
            // Add subgoal
            discoveredSubgoals.add(node.addChild(data));
        }

        return;
    }

    private void selectSubgoalCandidates(MCTSNode<MacroData> node, int count, ArrayList<MCTSNode<MacroData>> bucket)
    {
        if(count == k)
        {
            if(node.visitCount > 0)
                bucket.add(node);
            return;
        }

        count++;
        for(MCTSNode<MacroData> child : node.children)
        {
            selectSubgoalCandidates(child, count, bucket);
        }
    }

    private void advanceState(Game state, int action)
    {
        int waypointsBefore = state.getWaypointsVisited();
        state.tick(action);
        state.tick(action);
        state.tick(action);
        state.tick(action);
        state.tick(action);
        state.tick(action);
        state.tick(action);
        state.tick(action);
        state.tick(action);
        state.tick(action);
        int waypointsAfter = state.getWaypointsVisited();
        rewardAccumulator.addReward(waypointsBefore != waypointsAfter ? (waypointsAfter - waypointsBefore) * 100 : -1);

    }

    private void advanceStateMacro(MCTSNode<SubgoalData> subgoalNode, Game state, int action)
    {
        distanceMetric.toLatent(state, latentCache);
        double distanceBefore = latentDist(subgoalNode.data.latentState, latentCache);
        advanceState(state, action);
        distanceMetric.toLatent(state, latentCache);
        double distanceAfter = latentDist(subgoalNode.data.latentState, latentCache);
        macroAccumulator.addReward(distanceAfter - distanceBefore);
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
