package controllers.WoxController;

import framework.core.Controller;
import framework.core.Game;
import framework.core.PTSPConstants;
//import wox.serial.Easy;

import javax.swing.text.Position;
import java.util.Random;

/**
 * This is a sample controller that uses the WOX library for XML Serialized objects.
 * For more information about this library, included in the software, go to:
 *        http://woxserializer.sourceforge.net/
 *
 *
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class WoxController extends Controller
{
    /**
     * Random number generator
     */
    Random m_rnd;

    /**
     *  Bias the random in certain direction
     */
    Bias m_bias;

    /**
     * Default constructor, receiving a copy of the game state.
     * @param a_gameCopy copy of the game state.
     */
    public WoxController(Game a_gameCopy, long a_timeDue)
    {
        m_rnd = new Random();

        //How to read an XML object file:
        String fileName = "src/controllers/WoxController/evolved.xml";     //File to read from.
        this.m_bias = (Bias) Easy.load(fileName);                          //Reference assignation.

        //System.out.println(m_bias.getThrust() + " " + m_bias.getStraight() + " " + m_bias.getRight());
        
        //This way, we can save the object to the filename specified:
        //Easy.save(this.m_bias,"src/controllers/WoxController/evolvedOutput.xml");
    }

    /**
     * This function is called every execution step to get the action to execute.
     * @param a_gameCopy Copy of the current game state.
     * @param a_timeDue The time the next move is due
     * @return the integer identifier of the action to execute (see interface framework.core.Controller for definitions)
     */
    public int getAction(Game a_gameCopy, long a_timeDue)
    {
        boolean thrust = false;
        int turn = 0;

        //Check if we have to thrust.
        if(m_rnd.nextDouble() < m_bias.getThrust())
        {
            thrust = true;
        }

        //Go straight?
        if(m_rnd.nextDouble() > m_bias.getStraight())
        {
            //If not, decide where do we turn to:
            if(m_rnd.nextDouble() < m_bias.getRight())
                turn = 1;
            else
                turn = -1;
        }

        //Get the action from these inputs and return it.
        int action = Controller.getActionFromInput(thrust,turn);
        return action;
    }
}
