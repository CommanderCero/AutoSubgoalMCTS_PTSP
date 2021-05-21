package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Controller;
import framework.core.Game;

import java.util.Random;

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

    public BaseAction()
    {
        this.lowLevelAction = -1;
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

    public void sample(Random rng)
    {
        lowLevelAction = rng.nextInt(Controller.NUM_ACTIONS);
    }
}
