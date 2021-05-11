package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Game;

public class BaseAction
{
    static int DEFAULT_REPETITIONS = 15;

    public int lowLevelAction;
    public int repetitions;

    public BaseAction(int action)
    {
        this.lowLevelAction = action;
        repetitions = DEFAULT_REPETITIONS;
    }

    public void apply(Game state)
    {
        for(int i = 0; i < repetitions; i++)
        {
            state.tick(lowLevelAction);
        }
    }

    public double apply(RewardGame state)
    {
        double sumBefore = state.getRewardSum();
        for(int i = 0; i < repetitions; i++)
        {
            state.tick(lowLevelAction);
        }
        return state.getRewardSum() - sumBefore;
    }

    public void apply(RewardGame state, RewardAccumulator accumulator)
    {
        accumulator.addReward(apply(state));
    }
}
