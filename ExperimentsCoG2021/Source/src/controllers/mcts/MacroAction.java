package controllers.mcts;

import framework.core.Controller;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 16/10/12
 */
public class MacroAction
{
    public boolean m_thrust;
    public int m_steer;
    public int m_repetitions;

    public MacroAction(boolean a_t, int a_s, int a_rep)
    {
        m_thrust = a_t;
        m_steer = a_s;
        m_repetitions = a_rep;
    }

    public MacroAction(int a_action, int a_rep)
    {
    	//System.err.println(a_action);
        m_thrust = Controller.getThrust(a_action);
        m_steer = Controller.getTurning(a_action);
        m_repetitions = a_rep;
    }

    public int buildAction()
    {
        return Controller.getActionFromInput(m_thrust, m_steer);
    }

    public static int mutateThrust(int a_action)
    {
        boolean thrust = Controller.getThrust(a_action);
        int steer = Controller.getTurning(a_action);
        thrust = !thrust;
        return Controller.getActionFromInput(thrust, steer);
    }

    public static int mutateSteer(int a_action, boolean a_rightWise)
    {
        boolean thrust = Controller.getThrust(a_action);
        int steer = Controller.getTurning(a_action);
        if(steer == -1 || steer == 1)
            steer = 0;
        else if(steer == 0)
        {
            if(a_rightWise)
                steer = 1;
            else
                steer = -1;
        }
        return Controller.getActionFromInput(thrust, steer);
    }



}
