package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Controller;
import framework.core.Game;

import java.util.Random;

public abstract class AbstractController extends Controller
{
    enum StopCondition
    {
        Time,
        ForwardCalls
    }

    // Modify these parameters to adjust the conditions for all agents
    public static Random rng = new Random(2);
    public StopCondition stopCondition = StopCondition.ForwardCalls;
    public int maxForwardCalls = 80000;

    @Override
    public int getAction(Game a_game, long dueTimeMs)
    {
        RewardGame.resetCalls();
        RewardGame game = new NaiveRewardGame(a_game);
        if(stopCondition == StopCondition.Time)
        {
            long startTime = System.nanoTime();
            long timeBudgetMs = dueTimeMs - System.currentTimeMillis();
            // Run until the timeBudget is used up, with a little bit of remaining time to collect the action
            while((System.nanoTime() - startTime) / 1000000 < timeBudgetMs - 5)
            {
                step(game.getCopy());
            }
            System.out.println(RewardGame.getCalls());
        }
        else if(stopCondition == StopCondition.ForwardCalls)
        {
            while(game.getCalls() < maxForwardCalls)
            {
                step(game.getCopy());
            }
        }
        System.out.println("FMCalls: " + RewardGame.getCalls());
        return getBestAction();
    }

    protected abstract void step(RewardGame game);
    protected abstract int getBestAction();
}
