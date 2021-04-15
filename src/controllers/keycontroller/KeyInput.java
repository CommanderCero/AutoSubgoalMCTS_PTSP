package controllers.keycontroller;

import framework.core.Controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * This class is used to manage the key input.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class KeyInput extends KeyAdapter
{
    /**
     * Indicates if the thrust is pressed.
     */
    private boolean m_thrust;

    /**
     * Indicates if the turn must be applied.
     */
    private int m_turn;


    /**
     * Returns the action based on what keys are pressed.
     * @return the id of the action to execute.
     */
    public int getAction()
    {
        return Controller.getActionFromInput(m_thrust, m_turn);
    }


    /**
     * Manages KeyPressed events
     * @param e the event.
     */
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) {
            m_thrust = true;
        }
        if (key == KeyEvent.VK_LEFT) {
            m_turn = -1;
        }
        if (key == KeyEvent.VK_RIGHT) {
            m_turn = 1;
        }
    }

    /**
     * Manages keyReleased events
     * @param e the event.
     */
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) {
            m_thrust = false;
        }
        if (key == KeyEvent.VK_LEFT) {
            m_turn = 0;
        }
        if (key == KeyEvent.VK_RIGHT) {
            m_turn = 0;
        }
    }
}
