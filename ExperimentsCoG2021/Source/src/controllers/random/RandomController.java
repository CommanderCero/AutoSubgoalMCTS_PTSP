package controllers.random;

import framework.core.Controller;
import framework.core.Game;
import framework.utils.Vector2d;

import java.util.Random;

/**
 * This is a sample controller, that applies a random action at each step.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class RandomController extends Controller
{
    /**
     * Random number generator
     */
    Random m_rnd;

    /**
     * Default constructor, receiving a copy of the game state.
     * @param a_gameCopy copy of the game state.
     */
    public RandomController(Game a_gameCopy, long a_timeDue)
    {
        m_rnd = new Random();
        //waitRandom(PTSPConstants.INIT_TIME_MS);

    }

    /**
     * This function is called every execution step to get the action to execute.
     * @param a_gameCopy Copy of the current game state.
     * @param a_timeDue The time the next move is due
     * @return the integer identifier of the action to execute (see interface framework.core.Controller for definitions)
     */
    public int getAction(Game a_gameCopy, long a_timeDue)
    {
        //waitRandom(PTSPConstants.ACTION_TIME_MS);
        int action = m_rnd.nextInt(Controller.NUM_ACTIONS);
        return action;
    }

    /**
     * Waits for an amount of time, close to the on specified, before returning. It is used to check what happens if the
     * controller responds late.
     * @param a_near Time around this method waits.
     */
    private void waitRandom(int a_near)
    {
        int waitTime = (a_near-30) + m_rnd.nextInt(50);
        long startingTime = System.currentTimeMillis();
        long finalDateMs = startingTime + waitTime;

        while(startingTime < finalDateMs)
            startingTime = System.currentTimeMillis();

        //System.out.println("Waited " + waitTime + " ms");
    }
}
