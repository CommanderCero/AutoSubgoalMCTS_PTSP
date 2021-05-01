package controllers.autoSubgoalMCTS.RewardGames;

import framework.core.Game;

public class NaiveRewardGame extends RewardGame
{
    public NaiveRewardGame(Game initialState)
    {
        super(initialState);
    }

    private NaiveRewardGame()
    {
        super();
    }

    @Override
    public double evaluate()
    {
        return 0;
    }

    @Override
    protected void tickInternal(Game state, int action)
    {
        int waypointsBefore = state.getWaypointsVisited();
        state.tick(action);
        int deltaWaypoints = state.getWaypointsVisited() - waypointsBefore;

        addReward(deltaWaypoints > 0 ? (100 * deltaWaypoints) : -1);
    }

    @Override
    protected RewardGame copyInternal()
    {
        NaiveRewardGame copy = new NaiveRewardGame();
        return copy;
    }
}
