package controllers.heuristic;

import controllers.heuristic.graph.Graph;
import controllers.heuristic.graph.Node;
import controllers.heuristic.graph.Path;
import controllers.heuristic.graph.SightPath;
import framework.core.Game;
import framework.core.PTSPConstants;
import framework.core.Ship;
import framework.utils.Vector2d;

import java.util.LinkedList;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 22/08/12
 */
public abstract class HeuristicSolver {

    /**
     * Graph for the paths
     */
    public Graph m_graph;

    /**
     *  TSP Graph
     */
    public TSPSolver m_tsp;

    /**
     * Best route found.
     */
    public int[] m_bestRoute;

    public static LinkedList<Integer> m_inSightNodeList;
    public static LinkedList<Vector2d> m_inSightVectorList;
    public static final double CONSTANT = 0.156517643;


    public abstract int estimateSolutionTime(Game a_game);


    public static HeuristicSolver getSolver(String a_name)
    {
        if(a_name.equalsIgnoreCase("HeuristicNearest"))
            return new HeuristicNearest();
        if(a_name.equalsIgnoreCase("HeuristicNormalTSP"))
            return new HeuristicNormalTSP();
        if(a_name.equalsIgnoreCase("HeuristicPhysicsTSP"))
            return new HeuristicPhysicsTSP();
        if(a_name.equalsIgnoreCase("HeuristicPhysicsTSPMulti"))
            return new HeuristicPhysicsTSPMulti();

        return null;
    }


    public static int getCost(int[] a_bestRoute, Graph a_graph, Game a_game)
    {
        SightPath sps[] = new SightPath[a_bestRoute.length];
        m_inSightNodeList = new LinkedList<Integer>();
        m_inSightVectorList = new LinkedList<Vector2d>();

        //Get the sight path for every path between waypoints in the order of the path obtained.
        for(int index = 0; index < sps.length; ++index)
        {
            Node originIDNode = (index == 0) ? a_graph.getClosestNodeTo(a_game.getMap().getStartingPoint().x, a_game.getMap().getStartingPoint().y)
                    : a_graph.getClosestNodeTo(a_game.getWaypoints().get(a_bestRoute[index - 1]).s.x, a_game.getWaypoints().get(a_bestRoute[index - 1]).s.y);
            Node destIDNode = a_graph.getClosestNodeTo(a_game.getWaypoints().get(a_bestRoute[index]).s.x, a_game.getWaypoints().get(a_bestRoute[index]).s.y);
            Path toNextWaypoint  = a_graph.getPath(originIDNode.id(), destIDNode.id());
            sps[index] = new SightPath(toNextWaypoint,a_graph,a_game);

            //Now, add the nodes to the list:
            m_inSightNodeList.add(originIDNode.id()); //Origin always in.
            for(int i = 0; i < sps[index].midDistances.size()-1; ++i)
            {
                int idx = sps[index].midPoints.get(i);
                int nodeId = sps[index].p.m_points.get(idx);
                m_inSightNodeList.add(nodeId);
            }

            if(index == sps.length-1)
                m_inSightNodeList.add(destIDNode.id());
        }


        for(int i = 0; i < m_inSightNodeList.size()-1; ++i)
        {
            Node n_org = a_graph.getNode(m_inSightNodeList.get(i));
            Node n_dest = a_graph.getNode(m_inSightNodeList.get(i+1));

            Vector2d dest = new Vector2d (n_dest.x(), n_dest.y());
            Vector2d dir = dest.subtract(new Vector2d (n_org.x(), n_org.y()));
            dir.normalise();

            m_inSightVectorList.add(dir);
        }

        int numPoints = m_inSightNodeList.size();
        double speed = 0;
        int ticks = 0;
        //All points are in a straight line distance to the next.
        for(int i = 0; i < numPoints-1; ++i)
        {
            Node p_org = a_graph.getNode(m_inSightNodeList.get(i));
            Node p_dest = a_graph.getNode(m_inSightNodeList.get(i + 1));
            double distance = p_org.euclideanDistanceTo(p_dest);

            //System.out.format("d: %.3f, in. speed: %.3f,", distance, speed) ;
            while(distance > 0)
            {
                double newSpeed = (Ship.loss*speed + PTSPConstants.T * 0.025);
                distance -= newSpeed;
                speed = newSpeed;
                ticks++;
            }

            //Adjust the speed to turn
            if(i < numPoints-2)
            {
                Vector2d to = m_inSightVectorList.get(i);
                Vector2d from = m_inSightVectorList.get(i+1);
                double dot = to.dot(from);
                double penalization = pen_func(dot);
                speed *= penalization;

            }
        }

        return ticks;
    }


    public static double pen_func(double a_x)
    {
        double d = (Math.exp(a_x+1) - 1) * CONSTANT;
        if(d < 0) return 0;
        if(d > 1) return 1;
        return d;
    }

    public static void analyseRoute(Game a_game, Graph a_graph, int[] a_route)
    {
        SightPath sps[] = new SightPath[a_route.length];
        m_inSightNodeList = new LinkedList<Integer>();
        m_inSightVectorList = new LinkedList<Vector2d>();

        //Get the sight path for every path between waypoints in the order of the path obtained.
        for(int index = 0; index < sps.length; ++index)
        {
            Node originIDNode = (index == 0) ? a_graph.getClosestNodeTo(a_game.getMap().getStartingPoint().x, a_game.getMap().getStartingPoint().y)
                    : a_graph.getClosestNodeTo(a_game.getWaypoints().get(a_route[index - 1]).s.x, a_game.getWaypoints().get(a_route[index - 1]).s.y);
            Node destIDNode = a_graph.getClosestNodeTo(a_game.getWaypoints().get(a_route[index]).s.x, a_game.getWaypoints().get(a_route[index]).s.y);
            Path toNextWaypoint  = a_graph.getPath(originIDNode.id(), destIDNode.id());
            sps[index] = new SightPath(toNextWaypoint,a_graph,a_game);

            //Now, add the nodes to the list:
            m_inSightNodeList.add(originIDNode.id()); //Origin always in.
            for(int i = 0; i < sps[index].midDistances.size()-1; ++i)
            {
                int idx = sps[index].midPoints.get(i);
                int nodeId = sps[index].p.m_points.get(idx);
                m_inSightNodeList.add(nodeId);
            }

            if(index == sps.length-1)
                m_inSightNodeList.add(destIDNode.id());
        }

        double[] distances = new double[m_inSightNodeList.size()-1];
        for(int i = 0; i < m_inSightNodeList.size()-1; ++i)
        {
            Node n_org = a_graph.getNode(m_inSightNodeList.get(i));
            Node n_dest = a_graph.getNode(m_inSightNodeList.get(i+1));

            Vector2d dest = new Vector2d (n_dest.x(), n_dest.y());
            Vector2d dir = dest.subtract(new Vector2d (n_org.x(), n_org.y()));
            distances[i] = dir.mag();
            dir.normalise();

            m_inSightVectorList.add(dir);
        }

        double[] angles = new double[m_inSightVectorList.size()-1];
        for(int i = 1; i < m_inSightVectorList.size(); ++i)
        {
            Vector2d v_org = m_inSightVectorList.get(i - 1);
            Vector2d v_dest = m_inSightVectorList.get(i);

            double angleRad = Math.acos(v_org.dot(v_dest));
            angles[i-1] = (180*angleRad)/Math.PI;
        }

        /*int max = Math.max(distances.length, angles.length);
        for(int i = 0; i < max; ++i)
        {
            if(i < distances.length)
                System.out.format("%.2f",distances[i]);
            System.out.print(",");
            if(i < angles.length)
                System.out.format("%.2f",angles[i]);

            System.out.println();
        }*/


    }


}
