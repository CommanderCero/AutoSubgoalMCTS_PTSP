package controllers.mcts;

import framework.core.Controller;
import framework.core.Game;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 26/03/12
 * Time: 16:21
 * To change this template use File | Settings | File Templates.
 */
public class MCTSNode
{
    /**
     * Parent of this node.
     */
    public MCTSNode m_parent;

    /**
     * Children of this node.
     */
    public ArrayList<MCTSNode> m_children;

    /**
     * Reference to the game.
     */
    public Game m_game;

    /**
     * Last move of this node.
     */
    public MacroAction m_lastMove;

    /**
     * Number of moves of this node.
     */
    public int m_numMoves;

    /**
     * Times this node has been executed.
     */
    public int m_visits;

    /**
     * Score value of the node
     */
    public double m_value=0;


    /**
     * Obstacle counter.
     */
    public int m_obstacleCounter;

    /**
     * Node depth in tree;
     */
    public int m_depth;
    
    
    /**
     * Constructor
     * @param a_game
     */
    public MCTSNode(Game a_game)
    {
        m_game=a_game;
        m_parent=null;
        m_children=new ArrayList<MCTSNode>();
        m_numMoves= Controller.NUM_ACTIONS;
        m_obstacleCounter = 0;
        m_lastMove=null;
        m_depth = 0;
    }

    public MCTSNode(Game a_game,MCTSNode a_parent,MacroAction a_movePlayed)
    {
        m_game=a_game;
        m_parent=a_parent;
        m_lastMove=a_movePlayed;
        m_children=new ArrayList<MCTSNode>();
        m_numMoves=Controller.NUM_ACTIONS;
        m_depth = a_parent.m_depth+1;

        int obs = a_game.getShip().getCollLastStep() ? 1 : 0;
        m_obstacleCounter = a_parent.m_obstacleCounter + obs;
    }
    
}
