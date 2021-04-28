package controllers.autoSubgoalMCTS;

import framework.core.Game;

public class RewardGame
{
    RewardGame(Game initialState)
    {
        currentState = initialState;
        rewardSum = 0;
    }

    double tick(int action)
    {
        int waypointsBefore = currentState.getWaypointsVisited();
        currentState.tick(action);
        int deltaWaypoints = currentState.getWaypointsVisited() - waypointsBefore;

        double reward = deltaWaypoints > 0 ? (10 * deltaWaypoints) : -1;
        rewardSum += reward;
        return reward;
    }

    boolean isEnded() {return currentState.isEnded();}
    double getRewardSum() { return rewardSum; }

    Game getState() { return currentState; }

    RewardGame getCopy()
    {
        RewardGame copy = new RewardGame(currentState.getCopy());
        copy.rewardSum = rewardSum;
        return copy;
    }

    private Game currentState;
    private double rewardSum;
}
