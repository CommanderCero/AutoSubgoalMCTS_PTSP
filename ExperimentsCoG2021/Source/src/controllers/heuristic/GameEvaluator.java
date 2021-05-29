package controllers.heuristic;

import controllers.heuristic.graph.Node;
import controllers.heuristic.graph.SightPath;
import controllers.heuristic.graph.Graph;
import framework.core.Game;
import framework.core.Waypoint;
import framework.utils.Vector2d;
import framework.ExecSync;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 17/10/12
 */
public class GameEvaluator
{
    TSPGraphPhysicsEst m_tspGraph;
    Graph m_graph;
    public static boolean m_score2Active;
    public static final double MAX_FITNESS = 32000;
    public static final double SCORE_PER_WAYPOINT = 1000;
    public static final double SCORE_PER_VELOCITY = 100;
    public static final int PEN_NOT_MATCH = 1;
    public static int MACRO_ACTION_LENGTH = 15; //10
    public static int[] m_nextWaypoints;

    public double bestScore = 0;

    public GameEvaluator(TSPGraphPhysicsEst tspGraph, Graph graph, boolean score2Active)
    {
        m_score2Active = score2Active;
        m_tspGraph = tspGraph;
        m_graph = graph;
    }

    public void updateNextWaypoints(Game a_gameState, int a_howMany)
    {
        m_nextWaypoints = null;
        try{
            LinkedList<Waypoint> waypoints = a_gameState.getWaypoints();

            int nVisited = a_gameState.getWaypointsVisited();
            if(nVisited != waypoints.size())
            {
                m_nextWaypoints = new int[Math.min(a_howMany, waypoints.size() - nVisited)];

                if(match(a_gameState.getVisitOrder(), m_tspGraph.getBestPath()))
                {
                    for(int i = 0; i < m_nextWaypoints.length; ++i)
                    {
                        m_nextWaypoints[i] = m_tspGraph.getBestPath()[nVisited+i];
                    }

                }else{

                    //We have to improvise.
                    //System.out.println("route mismatch");
                    boolean done = false;
                    int count = 0;
                    int[] bestPath = m_tspGraph.getBestPath();

                    for(int i = 0; !done && i < m_tspGraph.getBestPath().length; ++i)
                    {
                        Waypoint wP = a_gameState.getWaypoints().get(bestPath[i]);
                        if(!wP.isCollected())
                        {
                            m_nextWaypoints[count++] = bestPath[i];
                            if(count >= 2)
                                done = true;
                        }

                    }
                }

            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public double scoreGame(Game a_gameState)
    {
	    //ExecSync.NUM_EVALUATIONS++;
        if(m_score2Active)
            return score2(a_gameState);
        else return score(a_gameState);
    }

    public static boolean isEndGame(Game a_gameState)
    {
        if(m_score2Active)
            return isGameEnded(a_gameState);
        else
            return a_gameState.isEnded();
    }


    private double score(Game a_game)
    {
        Waypoint closestW = getNextWaypointInPath(a_game);
        int timeSpent = 0;
        double score = 0;
        if(closestW == null)
        {
            //All waypoints visited
            timeSpent = 10000 - a_game.getTotalTime();
            score = 10 * (a_game.getWaypointsVisited() * SCORE_PER_WAYPOINT + timeSpent);
        }else
        {
            controllers.heuristic.graph.Path p = getPathToWaypoint(a_game,closestW);

            //Points per number of waypoints (magnitude: 1XXX)
            double waypointsPoints = 0;
            if(match(a_game.getVisitOrder(), m_tspGraph.getBestPath()))
                waypointsPoints = a_game.getWaypointsVisited() * SCORE_PER_WAYPOINT;
            /*else
                waypointsPoints = -SCORE_PER_WAYPOINT * PEN_NOT_MATCH;   */

            //Distance points (magnitude: 1XX)
            double distancePoints = getDistanceScore(p);

            //Collision penalizations: (magnitude: 1XX)
            //double collPoints = a_obsCounter * (-10);


            timeSpent = 10000 - a_game.getTotalTime();

            score = waypointsPoints + distancePoints /*+ collPoints */+ timeSpent;
        }
        return score;
    }


    public double score2(Game a_gameState)
    {
        int timeSpent = 0;
        double score = 0;
        if(m_nextWaypoints == null)
        {
            //All waypoints visited
            timeSpent = 10000 - a_gameState.getTotalTime();
            score = 10 * (a_gameState.getWaypointsVisited() * SCORE_PER_WAYPOINT + timeSpent);
        }else
        {

            //Distance points (magnitude: 1XX)
            double distancePoints = 0;
            Waypoint w0 = a_gameState.getWaypoints().get(m_nextWaypoints[0]);
            Waypoint w1 = null;

            controllers.heuristic.graph.Path pathToFirst = getPathToWaypoint(a_gameState,w0);
            controllers.heuristic.graph.Path pathToSecond = null, pathFirstToSecond = null;
            if(m_nextWaypoints.length == 1)
            {
                //LAST WAYPOINT TO COLLECT
                distancePoints = getDistanceScore(pathToFirst);
            }else
            {
                w1 = a_gameState.getWaypoints().get(m_nextWaypoints[1]);
                if(w0.isCollected())
                {
                    pathToSecond = getPathToWaypoint(a_gameState,w1);
                    distancePoints = getDistanceScore(pathToSecond) + SCORE_PER_WAYPOINT*10;
                }else
                {
                    pathFirstToSecond = m_tspGraph.m_distSight[m_nextWaypoints[0]][m_nextWaypoints[1]].p;
                    distancePoints = getDistanceScore(pathToFirst)/* + getDistanceScore(pathFirstToSecond)*/;
                }
            }

            //Points per number of waypoints (magnitude: 1XXX)
            double waypointsPoints = 0;
            int nWaypoints = 0;
            if(match(a_gameState.getVisitOrder(), m_tspGraph.getBestPath()))
            {
                if(w0.isCollected())
                {
                     nWaypoints++;
                     waypointsPoints = SCORE_PER_WAYPOINT;
                }
                if(w1 != null && w1.isCollected())
                {
                     nWaypoints++;
                     waypointsPoints = SCORE_PER_WAYPOINT * 2;
                }

            }
            /*else
                waypointsPoints = -SCORE_PER_WAYPOINT;*/

            //Points per orientation.
            double velPoints = 0;
            /*if(a_gameState.getMap().LineOfSight(a_gameState.getShip().s,w0.s) && a_gameState.getShip().v.mag() > 0.1)
            {
                Vector2d shipVelocity = new Vector2d(a_gameState.getShip().d);
                Vector2d target = new Vector2d(w0.s);
                Vector2d shipToTarget = target.subtract(a_gameState.getShip().s);

                shipToTarget.normalise();
                shipVelocity.normalise();
                double dot = shipToTarget.dot(shipVelocity);

                velPoints = (dot+1)*SCORE_PER_VELOCITY;
                //System.out.println("velP: " + velPoints);
            }     */

            timeSpent = 10000 - a_gameState.getTotalTime();

            double colPen = a_gameState.getShip().getCollLastStep() ? -100: 0;

            score = waypointsPoints + distancePoints + velPoints + timeSpent + colPen;

            //System.out.format("%.3f = %.3f + %.3f + %d + %.3f\n", score, waypointsPoints, distancePoints, timeSpent, colPen);
           // System.out.println(colPen);

            /*if(pathToSecond == null)
                  System.out.format("Waypoints ("+nWaypoints+"): " + waypointsPoints + ", Distance: to first %.3f, " +
                          "to second: NULL, first-second: %.3f, points:  %.3f, TOTAL:  %.3f\n",
                          pathToFirst.m_cost, pathFirstToSecond.m_cost, distancePoints,  score );
            else if(pathFirstToSecond == null)
                  System.out.format("Waypoints ("+nWaypoints+"): " + waypointsPoints + ", Distance: to first %.3f, " +
                          "to second: %.3f, first-second: NULL, points:  %.3f, TOTAL:  %.3f\n",
                          pathToFirst.m_cost, pathToSecond.m_cost, distancePoints,  score );
            else
                  System.out.format("Waypoints ("+nWaypoints+"): " + waypointsPoints + ", Distance: to first %.3f, " +
                          "to second: %.3f, first-second: %.3f, points: %.3f, TOTAL: %.3f\n",
                          pathToFirst.m_cost, pathToSecond.m_cost, pathFirstToSecond.m_cost, distancePoints,  score );*/
        }
        if(score > bestScore) {
        	bestScore = score;
        }
        return score;
    }

    public static boolean isGameEnded(Game a_gameState)
    {
        if(m_nextWaypoints == null)
            return true;

        Waypoint w0 = a_gameState.getWaypoints().get(m_nextWaypoints[0]);
        if(m_nextWaypoints.length == 1 )
        {
            if(w0.isCollected())
                return true;
            else return false;
        }
        else
        {
            Waypoint w1 = a_gameState.getWaypoints().get(m_nextWaypoints[1]);
            return w0.isCollected() && w1.isCollected();
        }

    }

    public Waypoint getNextWaypointInPath(Game a_gameCopy)
    {
        try{
        LinkedList<Waypoint> waypoints = a_gameCopy.getWaypoints();
        int nVisited = a_gameCopy.getWaypointsVisited();
        if(nVisited == waypoints.size())
            return null;

        int next = m_tspGraph.getBestPath()[nVisited];
        return waypoints.get(next);
        }catch(Exception e){
            int a  = 0;
        }
        return null;
    }

    public double getDistanceScore(controllers.heuristic.graph.Path a_p)
    {
        double estMaxDistance = 10000;
        double distancePoints = estMaxDistance - a_p.m_cost;
        distancePoints = Math.max(distancePoints,0);
        return distancePoints;
    }

    public boolean match(ArrayList<Integer> a_followedOrder, int[] a_pathDesired)
    {
        int idx = 0;
        for (Integer i : a_followedOrder)
        {
            if(i != a_pathDesired[idx])
                return false;
            idx++;
        }
        return true;
    }

    private controllers.heuristic.graph.Path getPathToWaypoint(Game a_game, Waypoint a_closestW)
    {
        controllers.heuristic.graph.Node shipNode = m_graph.getClosestNodeTo(a_game.getShip().s.x, a_game.getShip().s.y);
        controllers.heuristic.graph.Node waypointNode = m_graph.getClosestNodeTo(a_closestW.s.x, a_closestW.s.y);
        return  m_graph.getPath(shipNode.id(), waypointNode.id());
    }


}
