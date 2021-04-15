package controllers.heuristic;

import controllers.heuristic.graph.Node;
import framework.core.Controller;
import framework.core.Game;
import framework.utils.Vector2d;

import java.awt.*;
import java.util.LinkedList;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 22/08/12
 */
public class TestController extends Controller
{
    public HeuristicSolver hs;

    public TestController(Game a_game, long a_due)
    {
        hs = new HeuristicNearest();
        int nnTime = hs.estimateSolutionTime(a_game);
        for(int i =0; i < hs.m_bestRoute.length; ++i) System.out.print(hs.m_bestRoute[i] + "-"); System.out.println();

        hs = new HeuristicNormalTSP();
        int nTspTime = hs.estimateSolutionTime(a_game);
        for(int i =0; i < hs.m_bestRoute.length; ++i) System.out.print(hs.m_bestRoute[i] + "-"); System.out.println();

        hs = new HeuristicPhysicsTSPMulti();
        int pTspTime = hs.estimateSolutionTime(a_game);
        for(int i =0; i < hs.m_bestRoute.length; ++i) System.out.print(hs.m_bestRoute[i] + "-"); System.out.println();

        System.out.println(nnTime + " " + nTspTime + " " + pTspTime);
    }

    /**
     * Compute the next move given a copy of the current game and a time the move has to be computed by.
     * This is the method contestants need to implement. Examples are available in the controllers package.
     * Your controller must be in a subpackage of controllers.
     *
     * @param a_game A copy of the current game
     * @param a_timeDue The time the next move is due
     * @return The move to be played (i.e., the move calculated by your controller)
     */
    public int getAction(Game a_game, long a_timeDue)
    {
        return 0;
    }

    /**
     * This is a debug function that can be used to paint on the screen.
     * @param a_gr Graphics device to paint.
     */
    public void paint(Graphics2D a_gr)
    {
        a_gr.setColor(Color.white);
        for(int i = 0; i < hs.m_inSightNodeList.size(); ++i)
        {
            int nodeIndex = hs.m_inSightNodeList.get(i);
            Node thisNode = hs.m_graph.getNode(nodeIndex);
            a_gr.drawString(""+i, (int)thisNode.x(), (int)thisNode.y()+5);
        }

    }

}
