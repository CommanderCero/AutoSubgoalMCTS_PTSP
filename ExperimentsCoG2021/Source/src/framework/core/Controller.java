package framework.core;

import java.awt.*;

/**
 * This class is the superclass of your controller. In contains the code required to run the
 * controller as a thread. In provides numerous methods that allow the main class to use the
 * controller in various different execution modes. Your controller only needs to provide the
 * code for the getAction() method.
 */
public abstract class Controller implements Runnable
{

    /**
     * Action that involves no thrust nor rotation.
     */
    public static final int ACTION_NO_FRONT = 0;

    /**
     * Action that involves no thrust but left rotation.
     */
    public static final int ACTION_NO_LEFT = 1;

    /**
     * Action that involves no thrust but right rotation.
     */
    public static final int ACTION_NO_RIGHT = 2;

    /**
     * Action that involves thrust but no rotation.
     */
    public static final int ACTION_THR_FRONT = 3;

    /**
     * Action that involves thrust and left rotation.
     */
    public static final int ACTION_THR_LEFT = 4;

    /**
     * Action that involves thrust and right rotation.
     */
    public static final int ACTION_THR_RIGHT = 5;

    /**
     * Precomputed constant for PI/2.
     */
    public static final double HALF_PI = Math.PI * 0.5;

    /**
     * Precomputed constant for PI/4.
     */
    public static final double QUARTER_PI = Math.PI * 0.25;

    /**
     * Number of actions for the ship.
     */
    public static final int NUM_ACTIONS = 6;

    /**
     * Indicates if the thread of the controller is alive.
     */
    private boolean m_alive;

    /**
     * Indicates if the controller has received notification of another game cycle.
     */
    private boolean m_wasSignalled;

    /**
     * Indicates if the controller has computed an action for the next cycle.
     */
    private boolean m_hasComputed;

    /**
     * Indicates where the controller is due to execute an action.
     */
    private long m_timeDue;

    /**
     * Reference to the game.
     */
    private Game m_game;

    /**
     * Last move executed by the controller.
     */
    private int m_lastMove;

    /**
     * Tick of the game when a move was requested.
     */
    private int m_tick;

    /**
     * Instantiates a new controller. The constructor initialises the class variables.
     */
    public Controller()
    {
        m_alive=true;
        m_wasSignalled=false;
        m_hasComputed=false;
        m_tick=0;
    }

    /**
     * Resets the state of the controller.
     */
    public synchronized void reset()
    {
        m_alive=true;
        m_wasSignalled=false;
        m_hasComputed=false;
    }

    /**
     * Terminates the controller: a signal is sent and the flag 'alive' is set to false. When
     * the thread wakes up, the outer loop will terminate and the thread finishes.
     */
    public final synchronized void terminate()
    {
        m_alive=false;
        m_wasSignalled=true;
        notify();
    }

    /**
     * Updates the game state: a copy of the game is passed to this method and the class variable is
     * updated accordingly.
     *
     * @param a_game A copy of the current game
     * @param a_timeDue The time the next move is due
     */
    public final synchronized void update(Game a_game,long a_timeDue)
    {
        //System.out.println("update");
        this.m_game=a_game;
        this.m_timeDue=a_timeDue;
        m_wasSignalled=true;
        m_hasComputed=false;
        m_lastMove = Controller.ACTION_NO_FRONT;
        notify();
    }

    /* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
    public final void run()
    {
        while(m_alive)
        {
            synchronized(this)
            {
                while(!m_wasSignalled)
                {
                    try
                    {
                        wait();
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

                if(m_alive)
                {
                    m_wasSignalled=false;

                    new Thread(){
                        public void run(){
                            int t = m_tick;
                            int move = getAction(m_game, m_timeDue);
                            if(t == m_tick)
                            {
                                m_hasComputed=true;
                                m_lastMove=move;
                            }
                        }
                    }.start();

                }
            }

        }
    }


    /**
     * Retrieves the move from the controller (whatever is stored in the class variable).
     *
     * @return The move stored in the class variable 'lastMove'
     */
    public final int getMove()
    {
        m_tick++;
        return m_lastMove;
    }

    /**
     * This method is used to check whether the controller computed a move since the last
     * update of the game.
     *
     * @return Whether or not the controller computed a move since the last update
     */
    public final boolean hasComputed()
    {
        return m_hasComputed;
    }

    /**
     * Indicates if the action given implies acceleration
     * @param a_actionId Identifier of the action questioned.
     * @return true or false depending on the action given.
     */
    public static boolean getThrust(int a_actionId)
    {
        if(a_actionId >= Controller.ACTION_THR_FRONT && a_actionId <= Controller.ACTION_THR_RIGHT)
            return true;

        return false;
    }


    /**
     * Indicates if the action given implies rotation to the left (-1), to the right (1) or no rotation at all (0).
     * @param a_actionId The identifier of the action questioned.
     * @return rotation to the left (-1), to the right (1) or no rotation at all (0).
     */
    public static int getTurning(int a_actionId)
    {
        if(a_actionId == Controller.ACTION_NO_LEFT || a_actionId == Controller.ACTION_THR_LEFT)
            return -1;
        else if(a_actionId == Controller.ACTION_NO_RIGHT || a_actionId == Controller.ACTION_THR_RIGHT)
            return 1;

        return 0; //ACTION_NO_FRONT and ACTION_THR_FRONT.
    }

    /**
     * Given two inputs (acceleration and rotation), this method returns the action that performs both inputs at once.
     * @param a_thrust Desired thrust value for the action.
     * @param a_turn Desired turn sense for the action.
     * @return the identifier of the action that performs the movement desired.
     */
    public static int getActionFromInput(boolean a_thrust, int a_turn)
    {
        if(a_thrust)
        {
            if(a_turn == -1)
                return Controller.ACTION_THR_LEFT;
            else if(a_turn == 1)
                return Controller.ACTION_THR_RIGHT;
            else
                return Controller.ACTION_THR_FRONT;
        }
        else
        {
            if(a_turn == -1)
                return Controller.ACTION_NO_LEFT;
            else if(a_turn == 1)
                return Controller.ACTION_NO_RIGHT;
            else
                return Controller.ACTION_NO_FRONT;
        }

    }

    /**
     * This is a debug function that can be used to paint on the screen.
     * @param a_gr Graphics device to paint.
     */
    public void paint(Graphics2D a_gr){}


    /**
     * Compute the next move given a copy of the current game and a time the move has to be computed by.
     * This is the method contestants need to implement. Examples are available in the controllers package.
     * Your controller must be in a subpackage of controllers.
     *
     * @param a_game A copy of the current game
     * @param a_timeDue The time the next move is due
     * @return The move to be played (i.e., the move calculated by your controller)
     */
    public abstract int getAction(Game a_game, long a_timeDue);

}
