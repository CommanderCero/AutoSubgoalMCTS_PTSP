package controllers.autoSubgoalMCTS;

import framework.core.Controller;
import framework.core.Game;

import java.util.ArrayList;
import java.util.Collections;

public class AutoSubgoalMCTS
{
    private Game initialState;
    private MCTSNode<SubgoalData> root;
    private SubgoalSearchMCTS subgoalSearch;
    private int n;

    private RewardAccumulator rewardAccumulator;
    private RewardAccumulator macroAccumulator;

    public double explorationRate = Math.sqrt(2);
    public int maxSimulationSteps = 100;

    public AutoSubgoalMCTS(Game initialState, SubgoalSearchMCTS subgoalSearch, int n)
    {
        this.initialState = initialState.getCopy();
        this.root = new MCTSNode<>(new SubgoalData());
        this.root.data.subgoalSearch = subgoalSearch;
        this.n = n;

        rewardAccumulator = new RewardAccumulator(0.99);
        macroAccumulator = new RewardAccumulator(0.99);
    }

    public void setInitialState(Game newInitialState)
    {
        initialState = newInitialState;
    }

    public void step()
    {
        // Selection
        Game state = initialState.getCopy();
        MCTSNode<SubgoalData> currNode = root;
        while (!currNode.isLeafNode())
        {
            currNode = currNode.selectUCT(explorationRate);
            for(BaseAction action : currNode.data.macroAction)
            {
                advanceState(state, action);
            }
        }

        if (!state.isEnded())
        {
            // Expansion
            if(currNode.visitCount < n)
            {
                currNode.data.subgoalSearch.step(state);
            }
            else
            {
                currNode.data.subgoalSearch.addSubgoals(currNode);
                for(MCTSNode<SubgoalData> child : currNode.children)
                    child.data.subgoalSearch = currNode.data.subgoalSearch.initNewSearch();
                currNode.data.subgoalSearch = null;
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
            // Only one action left, aka this is our new root
            if(root.children.get(0).data.macroAction.size() == 1)
            {
                root = root.children.get(0);
                root.data.macroAction.clear(); // Clear to avoid confusion
            }
            else
            {
                root.children.get(0).data.macroAction.remove(0);
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

    private void advanceState(Game state, BaseAction action)
    {
        int waypointsBefore = state.getWaypointsVisited();
        action.apply(state);
        int waypointsAfter = state.getWaypointsVisited();
        rewardAccumulator.addReward(waypointsBefore != waypointsAfter ? (waypointsAfter - waypointsBefore) * 10 : -1);
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
