package controllers.mcts;


import java.util.ArrayList;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 17/10/12
 */
public class AdvancedMacroAction
{
    public int[] m_actions;

    public AdvancedMacroAction(ArrayList<Integer> a_actions, int a_numActions)
    {
        m_actions = new int[a_numActions];
        for(int i = 0; i < a_numActions; ++i)
             m_actions[i] = a_actions.get(i);
    }

    public AdvancedMacroAction(MacroAction a_ma)
    {
        m_actions = new int[a_ma.m_repetitions];
        int action = a_ma.buildAction();
        for(int i = 0; i < a_ma.m_repetitions; ++i)
             m_actions[i] = action;
    }

    public int getAction(int a_index)
    {
        return m_actions[a_index];
    }


    public String toString()
    {
        String st = "[";
        for(int i = 0; i < m_actions.length; ++i)
            st += this.m_actions[i];
        return st + "]";
    }


}
