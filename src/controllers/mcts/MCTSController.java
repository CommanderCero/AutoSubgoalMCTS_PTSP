package controllers.mcts;

import controllers.heuristic.GameEvaluator;
import framework.core.Controller;
import framework.core.Game;
import framework.utils.Vector2d;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 26/03/12
 * Time: 16:10
 * To change this template use File | Settings | File Templates.
 */
public class MCTSController extends Controller
{
    
    private MCTS m_mcts;
    private Vector2d[] m_waypointLocations;
    private Game m_game;
    private int m_currentMacroAction;
    private MacroAction m_lastAction;
    public boolean m_throwTree;
    
    public MCTSController(Game a_gameCopy, long a_timeDue)
    {
        m_currentMacroAction = 0;
        m_throwTree = true;
        m_lastAction = null;
        m_mcts = new MCTS(a_gameCopy, a_timeDue);
        m_waypointLocations = new Vector2d[a_gameCopy.getWaypoints().size()];
        for(int i = 0; i < m_waypointLocations.length; ++i)
            m_waypointLocations[i] = a_gameCopy.getWaypoints().get(i).s.copy();
        
    }
    
    public int getAction(Game a_gameCopy, long a_timeDue)
    {
        m_game = a_gameCopy.getCopy();
        MacroAction nextAction;
        int cycle = m_game.getTotalTime();
        //a_timeDue -=30;
        if(cycle == 0)
        {
            MacroAction action = m_mcts.execute(a_gameCopy, a_timeDue, m_throwTree);
        	//System.err.println(m_mcts.m_gameEvaluator.bestScore + " ----------" );
			//System.exit(0);
            m_lastAction = action;
            nextAction = action;
            m_throwTree = true;
            m_currentMacroAction = m_lastAction.m_repetitions-1;
        }else{
            prepareGameCopy(a_gameCopy);
            //if(m_currentMacroAction == m_lastAction.m_repetitions-1)
            if(m_currentMacroAction > 0)
            {
                m_mcts.execute(a_gameCopy, a_timeDue, m_throwTree);
                nextAction = m_lastAction;
                m_currentMacroAction--;
                m_throwTree = false;
            }else if(m_currentMacroAction == 0)
            {
                MacroAction action = m_mcts.execute(a_gameCopy, a_timeDue, m_throwTree);
                nextAction = m_lastAction;
                m_lastAction = action;
                if(m_lastAction != null)
                    m_currentMacroAction = m_lastAction.m_repetitions-1;
                m_throwTree = true;
            }else{
                throw new RuntimeException("This should not be happening: " + m_currentMacroAction);
            }
        }

        //System.out.println(" ----> EXECUTING: " + m_mcts.m_rootNode.m_visits);
        return nextAction.buildAction();
    }


    public void prepareGameCopy(Game a_game)
    {
        if(m_lastAction != null)
        {
            int first = GameEvaluator.MACRO_ACTION_LENGTH - m_currentMacroAction - 1;
            for(int i = first; i < GameEvaluator.MACRO_ACTION_LENGTH; ++i)
            {
                int singleMove = m_lastAction.buildAction();
                a_game.tickRandom(singleMove);
            }
        }
    }

    /**
     * This is a debug function that can be used to paint on the screen.
     * @param a_gr Graphics device to paint.
     */
    public void paint(Graphics2D a_gr)
    {
        //m_mcts.m_graph.draw(a_gr);
        /*a_gr.setColor(Color.yellow);
        Path pathToClosest = m_mcts.m_path;
        if(pathToClosest != null) for(int i = 0; i < pathToClosest.m_points.size()-1; ++i)
        {
            Node thisNode = m_mcts.m_graph.getNode(pathToClosest.m_points.get(i));
            Node nextNode = m_mcts.m_graph.getNode(pathToClosest.m_points.get(i+1));
            a_gr.drawLine(thisNode.x(), thisNode.y(), nextNode.x(),nextNode.y());
        }    */

        //PAINT ROUTE ORDER:
        a_gr.setColor(Color.yellow);
        int[] bestRoute = m_mcts.m_bestRoute;
        if(bestRoute != null)
        {
            for(int i = 0; i < bestRoute.length; ++i)
            {
                int waypointIndex = bestRoute[i];
                Vector2d thisWayPos = m_waypointLocations[waypointIndex];
                a_gr.drawString(""+i, (int)thisWayPos.x+5, (int)thisWayPos.y+5);
                if(i>0)
                {
                    int waypointLastIndex = bestRoute[i-1];
                    controllers.heuristic.graph.Node org = m_mcts.m_graph.getClosestNodeTo(m_waypointLocations[waypointLastIndex].x, m_waypointLocations[waypointLastIndex].y);
                    controllers.heuristic.graph.Node dest = m_mcts.m_graph.getClosestNodeTo(m_waypointLocations[waypointIndex].x, m_waypointLocations[waypointIndex].y);
                    controllers.heuristic.graph.Path p = m_mcts.m_graph.getPath(org.id(), dest.id());
                    if(p != null) for(int k = 0; k < p.m_points.size()-1; ++k)
                    {
                        controllers.heuristic.graph.Node thisNode = m_mcts.m_graph.getNode(p.m_points.get(k));
                        controllers.heuristic.graph.Node nextNode = m_mcts.m_graph.getNode(p.m_points.get(k+1));
                        //a_gr.drawLine(thisNode.x(), thisNode.y(), nextNode.x(),nextNode.y());

                        /*a_gr.setColor(Color.green);
                        ArrayList<Integer> midPoints = m_mcts.m_tspGraph.m_distSight[waypointLastIndex][waypointIndex].getOrder();
                        for(int m = 0; m < midPoints.size(); ++m)
                        {
                            int thisPoint = midPoints.get(m);
                            Node n = m_mcts.m_graph.getNode(p.m_points.get(thisPoint));
                            Vector2d nodePos = new Vector2d(n.x(), n.y());
                            a_gr.fillOval((int) nodePos.x, (int) nodePos.y, 5, 5);
                        }*/
                        a_gr.setColor(Color.white);
                    }
                }
            }

            /*a_gr.setColor(Color.green);
            Node org = m_mcts.m_graph.getClosestNodeTo(m_waypointLocations[bestRoute[2]].x, m_waypointLocations[bestRoute[2]].y);
            Node dest = m_mcts.m_graph.getClosestNodeTo(m_waypointLocations[bestRoute[3]].x, m_waypointLocations[bestRoute[3]].y);
            Path p = m_mcts.m_graph.getPath(org.id(), dest.id());
            ArrayList<Integer> midPoints = m_mcts.m_tspGraph.m_distSight[bestRoute[2]][bestRoute[3]].getOrder();
            for(int m = 0; m < midPoints.size(); ++m)
            {
                int thisPoint = midPoints.get(m);
                Node n = m_mcts.m_graph.getNode(p.m_points.get(thisPoint));
                Vector2d nodePos = new Vector2d(n.x(), n.y());
                a_gr.fillOval((int) nodePos.x, (int) nodePos.y, 5, 5);
            }   */
        }

        //PAINT TREE SEARCH:
        paintHeightMap(a_gr);

        //Paint the best MCTS route
        /*if(m_mcts.m_bestRouteSoFar != null && m_mcts.m_bestRouteSoFar.size()>0)
        {
            a_gr.setColor(Color.red);
            for(int i = 0; i < m_mcts.m_bestRouteSoFar.size() - 1; ++i)
            {
                Vector2d a = m_mcts.m_bestRouteSoFar.get(i);
                Vector2d b = m_mcts.m_bestRouteSoFar.get(i+1);
                a_gr.drawLine((int)Math.round(a.x),(int)Math.round(a.y),(int)Math.round(b.x),(int)Math.round(b.y));
            }
            //System.out.println("BEST ROUTE: " + m_mcts.m_bestRouteSoFar.size() + " starting with: " + m_mcts.m_bestActions.get(0));
        } */

        //Simulate for actions:
       /* if(m_mcts.m_bestActions != null && m_mcts.m_bestActions.size()>0)
        {
            a_gr.setColor(Color.yellow);
            ArrayList<Vector2d> followed = new ArrayList<Vector2d>();
            followed.add(m_game.getShip().s.copy());
            for(int i = 0; i < m_mcts.m_bestActions.size(); ++i)
            {
                m_game.tick(m_mcts.m_bestActions.get(i));
                followed.add(m_game.getShip().s.copy());
            }

            for(int i = 0; i < followed.size() - 1; ++i)
            {
                Vector2d a = followed.get(i);
                Vector2d b = followed.get(i+1);
                a_gr.drawLine((int)Math.round(a.x),(int)Math.round(a.y),(int)Math.round(b.x),(int)Math.round(b.y));
            }
            //System.out.println("BEST ROUTE: " + m_mcts.m_bestRouteSoFar.size() + " starting with: " + m_mcts.m_bestActions.get(0));
        } */





    }

    private void paintHeightMap(Graphics2D a_gr)
    {
        for(int i = 0; i < m_mcts.m_heightMap.length; ++i)
        {
            for(int j = 0; j < m_mcts.m_heightMap[0].length; ++j)
            {
                int height = m_mcts.m_heightMap[i][j];

                if(height > 0)
                {
                    Color col = getColorByHeight(height);
                    a_gr.setColor(col);
                    a_gr.fillRect(i,j,1,1);
                    //System.out.print(height + " ");
                }

            }
        }
    }

    private Color getColorByHeight(int height)
    {
        if(true)
        {
            if(height < 4)
                return new Color(218,15,240);  //PINK
            else if(height < 10)
                return new Color(11,14,241);   //PURPLE
            else if(height < 20)
                return new Color(13,53,242);    //BLUE
            else if(height < 30)
                return new Color(12,220,243);   //CYAN
            else if(height < 40)
                return new Color(63,245,10);    //GREEN
            else if(height < 60)
                return new Color(234,245,10);   //YELLOW
            else if(height < 80)
                return new Color(245,104,10);   //ORANGE

            return Color.red;                   //RED         */
        }else{
            if(height < 4)
                return Color.black;
            else if(height < 10)
                return new Color(50,50,50);
            else if(height < 20)
                return new Color(90,90,90);
            else if(height < 30)
                return new Color(140,140,140);
            else if(height < 40)
                return new Color(190,190,190);
            else if(height < 60)
                return new Color(210,210,210);
            else if(height < 80)
                return new Color(225,225,225);

            return new Color(250,250,250); //almost Color.white
        }
    }
    
}

