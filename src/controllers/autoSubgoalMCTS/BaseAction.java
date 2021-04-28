package controllers.autoSubgoalMCTS;

import framework.core.Game;

import java.util.Random;

public class BaseAction
{
    static int DEFAULT_REPETITIONS = 10;

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

    public void apply(RewardGame state)
    {
        for(int i = 0; i < repetitions; i++)
        {
            state.tick(lowLevelAction);
        }
    }
}
