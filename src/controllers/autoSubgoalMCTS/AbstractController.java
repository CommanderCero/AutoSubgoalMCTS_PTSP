package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Controller;
import framework.core.Game;

import java.util.Random;

public abstract class AbstractController extends Controller
{
    public enum StopCondition
    {
        Time,
        ForwardCalls
    }

    // Modify these parameters to adjust the conditions for all agents
    public static Random rng = new Random(2);
    public static StopCondition stopCondition = StopCondition.ForwardCalls;
    public static int maxForwardCalls = 70000;

    private BaseAction currAction = null;

    @Override
    public int getAction(Game a_game, long dueTimeMs)
    {
        RewardGame.resetCalls();

        if(stopCondition == StopCondition.Time)
        {
            long startTime = System.nanoTime();
            long timeBudgetMs = dueTimeMs - System.currentTimeMillis();
            // Run until the timeBudget is used up, with a little bit of remaining time to collect the action
            while((System.nanoTime() - startTime) / 1000000 < timeBudgetMs - 5)
            {
                RewardGame game = new NaiveRewardGame(a_game.getCopy());
                // If we are currently executing an action, start searching after we've executed it
                if(currAction != null)
                    currAction.apply(game);
                step(game.getCopy());
            }
        }
        else if(stopCondition == StopCondition.ForwardCalls)
        {
            RewardGame game = new NaiveRewardGame(a_game.getCopy());
            // If we are currently executing an action, start searching after we've executed it
            if(currAction != null)
                currAction.apply(game);
            while(game.getCalls() < maxForwardCalls)
            {
                step(game.getCopy());
            }
        }
        //System.out.println("FMCalls: " + RewardGame.getCalls());

        // Return an action
        if(currAction == null)
        {
            currAction = getBestAction();
        }
        int nextAction = currAction.lowLevelAction;
        currAction.repetitions--;
        if(currAction.repetitions == 0)
            currAction = null;
        return nextAction;
    }

    protected abstract void step(RewardGame game);
    protected abstract BaseAction getBestAction();
}
