package controllers.heuristic;

import controllers.heuristic.graph.Graph;
import controllers.heuristic.graph.Node;
import controllers.heuristic.graph.Path;
import framework.core.Game;
import framework.core.Waypoint;
import framework.utils.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 26/03/12
 * Time: 20:03
 * To change this template use File | Settings | File Templates.
 */
public class TSPGraphNearest extends TSPSolver
{
    /**
     * Node positions.
     */
    public HashMap<Integer,Vector2d> m_nodes;

    /**
     * Distances using A*.
     */
    public double[][] m_dists;

    /**
     * Distances from Origin
     */
    public double[] m_distOrigin;

    /**
     * Number of nodes in the map.
     */
    //public static final int MAX_NODES = 10;

    /**
     * BEST TSP path found so far.
     */
    public TSPPath m_tspBestPath;

    /**
     * Graph
     */
    public Graph m_graph;

    public Game m_game;

    /**
     * Creates the TSP Graph.
     * @param a_game Game to take the waypoints from.
     * @param a_graph Graph to take the costs
     */
    public TSPGraphNearest(Game a_game, Graph a_graph)
    {
        m_game = a_game;
        m_graph = a_graph;
        m_nodes = new HashMap<Integer, Vector2d>();
        m_dists = new double[a_game.getWaypoints().size()][a_game.getWaypoints().size()];
        m_distOrigin = new double[a_game.getWaypoints().size()];

        int index = 0;
        for(Waypoint way: a_game.getWaypoints())
        {
            m_nodes.put(index++, way.s.copy());
        }

        for(int i = 0; i < m_nodes.size(); ++i)
        {
            Vector2d a1 = m_nodes.get(i);
            for(int j = 0; j < m_nodes.size(); ++j)
            {
                if(i > j)
                {
                    Vector2d a2 = m_nodes.get(j);
                    double distance = getDistance(a1, a2);//a1.dist(a2);
                    
                    m_dists[i][j] = distance;
                    m_dists[j][i] = distance;

                }else if(i == j){
                    m_dists[i][i] = Double.MAX_VALUE;
                }
            }
        }

        Vector2d startingPoint = a_game.getMap().getStartingPoint();
        for(int i = 0; i < m_nodes.size(); ++i)
        {
            Vector2d a1 = m_nodes.get(i);
            double distance = getDistance(startingPoint, a1);//a1.dist(startingPoint);
            m_distOrigin[i] = distance;
        }
    }

    /**
     * Solves the TSP (Nearest Neighbour Algorithm).
     */
    public void solve()
    {
        //Create a default one, to be the best so far.
        int[] defaultBestPath = new int[m_game.getWaypoints().size()];
        for(int i =0; i < m_game.getWaypoints().size(); ++i)
            defaultBestPath[i] = i;
        double cost = getPathCost(defaultBestPath);
        m_tspBestPath = new TSPPath(m_game.getWaypoints().size(), defaultBestPath, cost);

        //Create an empty path to start with.
        int[] empty = new int[m_game.getWaypoints().size()];
        cost = 0;
        TSPPath emptyPath = new TSPPath(0, empty, cost);

        //And do the search (it updates m_tspBestPath)
        _search(emptyPath);
    }

    private double m_minCost = Double.MAX_VALUE;

    private void _search(TSPPath a_currentPath)
    {
        if(a_currentPath.m_nNodes == m_tspBestPath.m_nNodes)
        {
            //We have a path with all nodes in it. We are done.
            m_tspBestPath = a_currentPath;

        }else
        {
            int nodeIdx = -1;
            double cost = Double.MAX_VALUE;

            //Take all nodes...
            for(int i = 0; i < m_game.getWaypoints().size(); ++i)
            {
                //..  that are not included in a_currentPath.
                if(!a_currentPath.includes(i))
                {
                     //Get the cost to this new link.
                    double linkCost;
                    if(a_currentPath.m_nNodes == 0)
                    {
                        linkCost = m_distOrigin[i];
                    }else{
                        int lastNode = a_currentPath.m_path[a_currentPath.m_nNodes-1];
                        linkCost =  m_dists[lastNode][i];
                    }

                    if(linkCost < cost)
                    {
                        cost = linkCost;
                        nodeIdx = i;
                    }


                }
            }

            //Now, add the closest one to the path and keep searching
            double newCost = a_currentPath.m_totalCost + cost;
            if(newCost < m_tspBestPath.m_totalCost)
            {
                //search!
                TSPPath nextPath = new TSPPath(a_currentPath, nodeIdx, newCost, m_tspBestPath.m_nNodes);
                _search(nextPath);
            }

        }
    }

    /**
     * Gets the path distance from position a_org to a_dest
     * @param a_org  Origin
     * @param a_dest Destination
     * @return The distance.
     */
    private double getDistance(Vector2d a_org, Vector2d a_dest)
    {
        Node org = m_graph.getClosestNodeTo(a_org.x, a_org.y);
        Node dest = m_graph.getClosestNodeTo(a_dest.x, a_dest.y);
        Path p = m_graph.getPath(org.id(), dest.id());
        return p.m_cost;
    }

    /**
     * Gets the cost of a given path
     * @param a_path Path to get the cost.
     * @return the total cost.
     */
    private double getPathCost(int[] a_path)
    {
        int index = 0;
        double cost = 0;

        if(a_path[index] == -1)
            return -1;
        else cost = m_distOrigin[a_path[index]];
        index++;

        while(index < a_path.length && a_path[index] != -1)
        {
            double thisCost = m_dists[a_path[index-1]][a_path[index]];
            cost += thisCost;
            index++;
        }
        return cost;
    }
    
    public int[] getBestPath()
    {
        return m_tspBestPath.m_path;
    }

    public void improveOpt(){
        System.out.println("Not implemented.");
    }

    private class TSPPath
    {
        public int m_nNodes;
        public double m_totalCost;
        public int[] m_path;

        public TSPPath(int a_nNodes, int[] a_nodes, double a_totCost)
        {
            m_path = new int[a_nodes.length];
            m_nNodes = a_nNodes;
            m_totalCost =a_totCost;
            System.arraycopy(a_nodes, 0, m_path, 0, a_nNodes);
        }

        public TSPPath(TSPPath a_base, int a_newNode, double a_newCost, int a_nNodes)
        {
            m_path = new int[a_nNodes];
            m_nNodes = a_base.m_nNodes+1;
            m_totalCost =a_newCost;
            System.arraycopy(a_base.m_path, 0, m_path, 0, a_base.m_nNodes);
            m_path[m_nNodes-1] = a_newNode;
        }

        public boolean includes(int a_nodeId)
        {
            for(int i =0; i < m_nNodes; ++i)
                if(m_path[i] == a_nodeId)
                    return true;
            return false;
        }

    }

}
