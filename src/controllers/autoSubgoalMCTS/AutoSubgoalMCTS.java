package controllers.autoSubgoalMCTS;

import framework.core.Controller;
import framework.core.Game;

import java.util.ArrayList;
import java.util.Collections;

public class AutoSubgoalMCTS
{
    public class MacroData
    {
        public int action;
    };

    public class SubgoalData
    {
        public SubgoalData()
        {
            macroAction = new ArrayList<>();
        }

        Game state;
        int discardedSubgoalCount = 0;
        ArrayList<Integer> macroAction;
        MCTSNode<MacroData> macroRoot;
    };

    private Game initialState;
    private MCTSNode<SubgoalData> root;
    private IDistanceMetric distanceMetric;
    private int k;
    private double radius;
    private ArrayList<Game> discoveredSubgoals;

    private int distanceSampleCount;
    private double meanDistance;
    private RewardAccumulator rewardAccumulator;
    private RewardAccumulator macroAccumulator;

    public double explorationRate = Math.sqrt(2);
    public int maxSimulationSteps = 100;

    public AutoSubgoalMCTS(Game initialState, IDistanceMetric distanceMetric, int k, double radius)
    {
        this.initialState = initialState;
        this.root = new MCTSNode<>(new SubgoalData());
        this.root.data.state = initialState.getCopy();
        this.distanceMetric = distanceMetric;
        this.k = k;
        this.radius = radius;

        rewardAccumulator = new RewardAccumulator(1);
        macroAccumulator = new RewardAccumulator(0.99);

        discoveredSubgoals = new ArrayList<>();
        discoveredSubgoals.add(initialState.getCopy());
    }

    void step()
    {
        if (root.fullyExplored)
        {
            return;
        }

        // Selection
        Game state = initialState.getCopy();
        MCTSNode<SubgoalData> currNode = root;
        while (!currNode.isLeafNode() && currNode.data.macroRoot == null)
        {
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

    void extendNode(MCTSNode<SubgoalData> node, Game state)
    {
        if(node.data.macroRoot == null)
        {
            node.data.macroRoot = new MCTSNode<MacroData>(new MacroData());
        }

        // Selection
        MCTSNode<MacroData> currNode = node.data.macroRoot;
        while(!currNode.isLeafNode())
        {
            currNode = currNode.selectUCT(Math.sqrt(2));
            advanceStateMacro(node, state, currNode.data.action);
        }

        // Did we find a new subgoal?
        if(distanceMetric.computeDistance(node.data.state, state) / meanDistance >= radius)
        {
            boolean isValid = true;
            for (MCTSNode<SubgoalData> subgoal : node.children)
            {
                if (subgoal == node)
                    continue;

                if (distanceMetric.computeDistance(subgoal.data.state, state) / meanDistance < radius)
                {
                    isValid = false;
                    break;
                }
            }

            if (isValid)
            {
                // Compute n closest subgoals
                int n = discoveredSubgoals.size() > 3 ? 3 : discoveredSubgoals.size();
                Collections.sort(discoveredSubgoals, (Game v1, Game v2) ->
                {
                    double v2Dist = distanceMetric.computeDistance(v2, state);
                    double v1Dist = distanceMetric.computeDistance(v1, state);
                    return v1Dist > v2Dist ? 1 : v1Dist < v2Dist ? -1 : 0;
                });

                // Check if the subgoal is sufficiently far from the other n-subgoals
                double distSum = 0;
                for(int i =0; i < n; i++)
                {
                    distSum += distanceMetric.computeDistance(discoveredSubgoals.get(i), state) / meanDistance;
                }

                isValid = distSum >= radius * n;
            }

            // Add new subgoal
            if (isValid)
            {
                SubgoalData newSubgoal = new SubgoalData();
                newSubgoal.state = state.getCopy();
                MCTSNode<MacroData> tmpNode = currNode;
                while (tmpNode.parent != null)
                {
                    newSubgoal.macroAction.add(tmpNode.data.action);
                    tmpNode = tmpNode.parent;
                }
                Collections.reverse(newSubgoal.macroAction);
                node.addChild(newSubgoal);
                discoveredSubgoals.add(state);
            }
            else
            {
                ++node.data.discardedSubgoalCount;
                if(node.data.discardedSubgoalCount > 20)
                {
                    node.data.macroRoot = null;
                    if(node.children.isEmpty())
                        node.setFullyExplored();
                    return;
                }
            }

            currNode.setFullyExplored();
        }
        else if (!state.isEnded())
        {
            // Expansion
            for(int action = 0; action < Controller.NUM_ACTIONS; action++)
            {
                MacroData macroData = new MacroData();
                macroData.action = action;
                currNode.addChild(macroData);
            }

            currNode = currNode.children.get(0);
            advanceStateMacro(node, state, currNode.data.action);
        }

        // Backpropagation
        currNode.backpropagate(macroAccumulator.getRewardSum());
        macroAccumulator.reset();
    }

    void advanceState(Game state, int action)
    {
        state.tick(action);
        rewardAccumulator.addReward(-1);
    }

    void advanceStateMacro(MCTSNode<SubgoalData> subgoalNode, Game state, int action)
    {
        double distanceBefore = distanceMetric.computeDistance(subgoalNode.data.state, state);
        advanceState(state, action);
        double distanceAfter = distanceMetric.computeDistance(subgoalNode.data.state, state);
        macroAccumulator.addReward(distanceAfter - distanceBefore);

        distanceSampleCount += 1;
        meanDistance += (Math.abs(distanceAfter - distanceBefore) - meanDistance) / distanceSampleCount;
    }

    void replaceRoot(MCTSNode<SubgoalData> selectedChild, Game newInitialState)
    {
        // Reset subgoals
        discoveredSubgoals.clear();

        for(int i = 0; i < root.children.size(); i++)
        {
            if(root.children.get(i) == selectedChild)
            {
                root = root.detachChild(i);
                i--;
            }
            else
            {
                addSubgoals(root.children.get(i));
            }
        }

        initialState = newInitialState.getCopy();
    }

    MCTSNode<SubgoalData> getBestChild()
    {
        double maxScore = Double.MIN_VALUE;
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

    MCTSNode<SubgoalData> getRoot()
    {
        return root;
    }

    private void addSubgoals(MCTSNode<SubgoalData> node)
    {
        discoveredSubgoals.add(node.data.state);
        node.fullyExplored = false;
        node.data.discardedSubgoalCount = 0;
        for(MCTSNode<SubgoalData> child : node.children)
        {
            addSubgoals(child);
        }
    }
}
