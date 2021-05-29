package controllers.mcts;

import controllers.mcts.graph.Graph;
import controllers.mcts.graph.Node;
import controllers.mcts.graph.Path;
import controllers.mcts.graph.SightPath;
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
public class TSPGraphSight
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
     * Distances using A*.
     */
    public SightPath[][] m_distSight;

    /**
     * Distances from Origin
     */
    public SightPath[] m_distSightOrigin;

    /**
     * Distances from Origin
     */
    public double[] m_distOrigin;

    /**
     * Number of nodes in the map.
     */
    public static final int MAX_NODES = 10;

    /**
     * BEST TSP path found so far.
     */
    public TSPPath m_tspBestPath;

    /**
     * Graph
     */
    public Graph m_graph;


    /**
     * Creates the TSP Graph.
     * @param a_game Game to take the waypoints from.
     * @param a_graph Graph to take the costs
     */
    public TSPGraphSight(Game a_game, Graph a_graph)
    {
        m_graph = a_graph;
        m_nodes = new HashMap<Integer, Vector2d>();
        m_dists = new double[MAX_NODES][MAX_NODES];
        m_distSight = new SightPath[MAX_NODES][MAX_NODES];
        m_distOrigin = new double[MAX_NODES];
        m_distSightOrigin = new SightPath[MAX_NODES];

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
                    Path p = getDistance(a1, a2);
                    double distance = p.m_cost; //a1.dist(a2);

                    m_dists[i][j] = distance;
                    m_dists[j][i] = distance;
                    
                    m_distSight[i][j] = new SightPath(p,a_graph, a_game);
                    m_distSight[j][i] = new SightPath(m_distSight[i][j]);

                }else if(i == j){
                    m_dists[i][i] = Double.MAX_VALUE;
                }
            }
        }

        Vector2d startingPoint = a_game.getMap().getStartingPoint();
        for(int i = 0; i < m_nodes.size(); ++i)
        {
            Vector2d a1 = m_nodes.get(i);
            Path p = getDistance(startingPoint, a1);
            double distance = p.m_cost;//a1.dist(startingPoint);
            m_distOrigin[i] = distance;
            m_distSightOrigin[i] = new SightPath(p,a_graph, a_game);
        }
    }

    /**
     * Solves the TSP (Branch and Bound algorithm).
     */
    public void solve()
    {
        //Create a default one, to be the best so far.
        int[] defaultBestPath = new int[MAX_NODES];
        for(int i =0; i < MAX_NODES; ++i)
            defaultBestPath[i] = i;
        double cost = getPathCost(defaultBestPath);
        m_tspBestPath = new TSPPath(MAX_NODES, defaultBestPath, cost);

        //Create an empty path to start with.
        int[] empty = new int[MAX_NODES];
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
            //We have a path with all nodes in it. Check if m_tspBestPath needs to be updated.
            if(a_currentPath.m_totalCost < m_tspBestPath.m_totalCost)
            {
                if(a_currentPath.m_totalCost < m_minCost) m_minCost = a_currentPath.m_totalCost;
                m_tspBestPath = a_currentPath;
            }
        }else
        {
            //Take all nodes...
            for(int i = 0; i < MAX_NODES; ++i)
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
                        /*int lastNode = a_currentPath.m_path[a_currentPath.m_nNodes-1];
                        linkCost =  m_dists[lastNode][i]; */
                        linkCost = getLinkCost(a_currentPath,i);
                    }

                    //Build the new path
                    double newCost = a_currentPath.m_totalCost + linkCost;
                    if(newCost < m_tspBestPath.m_totalCost)
                    {
                        //search!
                        TSPPath nextPath = new TSPPath(a_currentPath, i, newCost);
                        _search(nextPath);
                    }
                }
            }
        }
    }


    private double getLinkCost(TSPPath a_currentPath, int nextNode)
    {
        int lastNode = a_currentPath.m_path[a_currentPath.m_nNodes-1];

        SightPath spFrom;
        int fromNode = -1;
        if(a_currentPath.m_nNodes > 1)
        {
            fromNode = a_currentPath.m_path[a_currentPath.m_nNodes-2];
            spFrom = m_distSight[fromNode][lastNode];
        }else
        {
            spFrom = m_distSightOrigin[lastNode];
        }

        SightPath spTo = m_distSight[lastNode][nextNode];

        double anglesFrom = spFrom.angleSum;
        double anglesTo = spTo.angleSum;
        double angleFromTo = spTo.getAngle(spFrom.last, spTo.first);

        int numPoints = spFrom.midPoints.size() + spTo.midPoints.size() + 1;
        double totAngle = anglesFrom + anglesTo + angleFromTo;

        double anglePerPoint = totAngle / numPoints;


        //double mult = 1 + MCTS.normalise(anglePerPoint,0, Math.PI);
        double norm = MCTS.normalise(anglePerPoint,0, Math.PI);
        double mult = Math.exp(norm);
        //double mult = scoreAngle(anglePerPoint);// + 1;
        
        //return m_dists[lastNode][nextNode];
        return m_dists[lastNode][nextNode] * mult;
    }

    public double scoreAngle(double anglePerPoint)
    {
           if(anglePerPoint < ARCCOS._PI4)
               return 0.0;
           else if(anglePerPoint < ARCCOS._PI2)
               return 0.5;
           else if(anglePerPoint < ARCCOS._PI)
               return 1.5;
           else if(anglePerPoint < ARCCOS._2PI3)
               return 2.5;
         else return 5;
    }


    /**
     * Gets the path distance from position a_org to a_dest
     * @param a_org  Origin
     * @param a_dest Destination
     * @return The path .
     */
    private Path getDistance(Vector2d a_org, Vector2d a_dest)
    {
        Node org = m_graph.getClosestNodeTo(a_org.x, a_org.y);
        Node dest = m_graph.getClosestNodeTo(a_dest.x, a_dest.y);
        return m_graph.getPath(org.id(), dest.id());
    }

    /**
     * Returns those nodes who are not in the path received.
     * @param a_path Path to check
     * @return array with the node indexes
     */
    private Integer[] getNotInPath(TSPPath a_path)
    {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        for(int i = 0; i < MAX_NODES; ++i)
        {
            if(!a_path.includes(i))
                arr.add(i);
        }
        return (Integer[]) arr.toArray();
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
    

    private class TSPPath
    {
        public int m_nNodes;
        public double m_totalCost;
        public int[] m_path;

        public TSPPath(int a_nNodes, int[] a_nodes, double a_totCost)
        {
            m_path = new int[MAX_NODES];
            m_nNodes = a_nNodes;
            m_totalCost =a_totCost;
            System.arraycopy(a_nodes, 0, m_path, 0, a_nNodes);
        }

        public TSPPath(TSPPath a_base, int a_newNode, double a_newCost)
        {
            m_path = new int[MAX_NODES];
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

    /**
     * Pair class
     * @param <A> This is A
     * @param <B> This is B
     */
    private class Pair<A,B>
    {
        public A first;
        public B second;

        public Pair(A first, B second) {
            super();
            this.first = first;
            this.second = second;
        }

        public int hashCode() {
            int hashFirst = first != null ? first.hashCode() : 0;
            int hashSecond = second != null ? second.hashCode() : 0;

            return (hashFirst + hashSecond) * hashSecond + hashFirst;
        }

        public boolean equals(Object other) {
            if (other instanceof Pair) {
                Pair otherPair = (Pair) other;
                return ((  this.first == otherPair.first ||
                        ( this.first != null && otherPair.first != null && this.first.equals(otherPair.first))) &&
                        (this.second == otherPair.second ||
                                ( this.second != null && otherPair.second != null &&
                                        this.second.equals(otherPair.second))) );
            }

            return false;
        }

        public String toString()
        {
            return "(" + first + ", " + second + ")";
        }

    }

}
