package controllers.keycontroller;
import framework.core.Controller;
import framework.core.Game;

/**
 * This class is used for the KeyController (human playing).
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class KeyController extends Controller
{
    /**
     * To manage the keyboard input.
     */
    private KeyInput m_input;

    /**
     * Constructor of the KeyController.
     */
    public KeyController()
    {
        m_input = new KeyInput();
    }

    /**
     * This function is called every execution step to get the action to execute.
     * @param a_gameCopy Copy of the current game state.
     * @param a_timeDue The time the next move is due
     * @return the integer identifier of the action to execute (see interface framework.core.Controller for definitions)
     */
    public int getAction(Game a_gameCopy, long a_timeDue)
    {
        return m_input.getAction();
    }

    /**
     * Return the input manager
     * @return the input manager
     */
    public KeyInput getInput() {return m_input;}
}
