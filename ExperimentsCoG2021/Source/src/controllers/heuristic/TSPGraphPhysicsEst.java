package controllers.heuristic;

import controllers.heuristic.graph.Graph;
import controllers.heuristic.graph.Node;
import controllers.heuristic.graph.Path;
import controllers.heuristic.graph.SightPath;
import framework.core.Game;
import framework.core.PTSPConstants;
import framework.core.Ship;
import framework.core.Waypoint;
import framework.utils.Vector2d;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 26/03/12
 * Time: 20:03
 * To change this template use File | Settings | File Templates.
 */
public class TSPGraphPhysicsEst extends TSPSolver
{
    /**
     * Number of nodes in the map.
     */
    public static int MAX_NODES = 10;

    /**
     * BEST TSP path found so far.
     */
    public TSPPath m_tspBestPath;

    /**
     * Graph
     */
    public Graph m_graph;

    /**
     * Game reference
     */
    public Game m_game;


    /**
     * Distances using A*.
     */
    public SightPath[][] m_distSight;

    /**
     * Distances from Origin
     */
    public SightPath[] m_distSightOrigin;

    /**
     * Node positions.
     */
    public HashMap<Integer,Vector2d> m_nodes;

    /**
     * Priority queue for suboptimal routes.
     */
    public TreeMap<Integer, Integer[]> m_paths;


    /**
     * Creates the TSP Graph.
     * @param a_game Game to take the waypoints from.
     * @param a_graph Graph to take the costs
     */
    public TSPGraphPhysicsEst(Game a_game, Graph a_graph)
    {
        m_game = a_game;
        m_graph = a_graph;
        m_paths = new TreeMap<Integer, Integer[]>();
        m_distSightOrigin = new SightPath[a_game.getWaypoints().size()];
        m_distSight = new SightPath[a_game.getWaypoints().size()][a_game.getWaypoints().size()];
        m_nodes = new HashMap<Integer, Vector2d>();

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
                    Path p = getPath(a1, a2);
                    m_distSight[i][j] = new SightPath(p,a_graph, a_game);
                    Path p2 = getPath(a2, a1);
                    m_distSight[j][i] = new SightPath(p2, a_graph, a_game);
                }
            }
        }

        Vector2d startingPoint = a_game.getMap().getStartingPoint();
        for(int i = 0; i < m_nodes.size(); ++i)
        {
            Vector2d a1 = m_nodes.get(i);
            Path p = getPath(startingPoint, a1);
            m_distSightOrigin[i] = new SightPath(p,a_graph, a_game);
        }
    }

    /**
     * Gets the path distance from position a_org to a_dest
     * @param a_org  Origin
     * @param a_dest Destination
     * @return The path .
     */
    private Path getPath(Vector2d a_org, Vector2d a_dest)
    {
        Node org = m_graph.getClosestNodeTo(a_org.x, a_org.y);
        Node dest = m_graph.getClosestNodeTo(a_dest.x, a_dest.y);
        return m_graph.getPath(org.id(), dest.id());
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
        double cost = HeuristicSolver.getCost(defaultBestPath, m_graph, m_game);
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
                    //Build the new path
                    TSPPath newPath = getLinkCost(a_currentPath,i);
                    double newCost = newPath.ticks;
                    if(newCost < m_tspBestPath.m_totalCost)
                    {
                        //only if it si better, search!
                        _search(newPath);
                    }
                }
            }
        }
    }

    private TSPPath getLinkCost(TSPPath a_currentPath, int a_nextWaypoint)
    {
        //We add all stuff in new path, copying from the path so far.
        TSPPath newPath = a_currentPath.getCopy();

        //This is hoy I come to this point (null at the beginning!).
        Vector2d lastVector = newPath.m_lastVector;

        SightPath spC; //This contains all information needed to go from waypoint A to B. Get it:
        if(lastVector == null)
        {
            spC = m_distSightOrigin[a_nextWaypoint];
        }
        else
        {
            int  lastWaypointId = a_currentPath.m_path[a_currentPath.m_nNodes-1];
            spC = m_distSight[lastWaypointId][a_nextWaypoint];
        }

        //Adjust the speed, towards the new destination, depending on the turn from how I came in. (if lastVector is null, no turn!)
        if(lastVector != null)
        {
            Vector2d to = spC.first; //spC.midVectors.get(0);    //This is the first vector in SightPath.
            double dot = lastVector.dot(to);
            double penalization = HeuristicSolver.pen_func(dot);  //Get penalization multiplier.
            newPath.m_speed *= penalization;                      //And adjust speed consequently.
        }

        //Now, for each turn in the path to the next waypoint, compute time and speed.
        for(int i = 0; i < spC.midDistances.size(); ++i)
        {
            double distance = spC.midDistances.get(i);    //This is the distance to the next turn in the SightPath

            while(distance > 0)    //Calculate ticks, updating speed with the Physics acceleration, until nxt point is reached.
            {
                double newSpeed = (Ship.loss*newPath.m_speed + PTSPConstants.T * 0.025);
                distance -= newSpeed;
                newPath.m_speed = newSpeed;
                newPath.ticks++;
            }
            //We have reached the new turn point in the SightPath.


            // Need to update the speed according to the turn shape.
            if(i < spC.midDistances.size()-1)
            {
                //Update speed.
                double dot = spC.midDots.get(i);    //This is the dot product between the i and i+1 vectors in the SightPath
                double penalization = HeuristicSolver.pen_func(dot);    //Get penalization multiplier.
                newPath.m_speed *= penalization;                        //And adjust speed consequently.
            }
        }

        //Update some attributes of the new path:
        newPath.m_path[newPath.m_nNodes] = a_nextWaypoint;
        newPath.m_nNodes++;
        newPath.m_totalCost = newPath.ticks;
        newPath.m_lastVector = spC.last.copy();

        return newPath;
    }


    public void swap(int[] a, int i, int j)
    {
        int v = a[j];
        a[j]=a[i];
        a[i]=v;
    }

    public void swap3(int[] a, int i, int j, int k)
    {
        int v = a[k];
        a[k]=a[j];
        a[j]=a[i];
        a[i]=v;
    }


    public void improveOpt()
    {
        int testArray[];
        int bestRoutePhy[] = new int[m_tspBestPath.m_path.length];
        int bestEstimate = HeuristicSolver.getCost(m_tspBestPath.m_path, m_graph, m_game);
        //System.out.println(" Initial BestEstimate: " + bestEstimate);

        System.arraycopy(m_tspBestPath.m_path,0,bestRoutePhy,0,m_tspBestPath.m_path.length);

        //Opt-2
        for(int i = 0; i < m_tspBestPath.m_path.length-1; ++i)
        {
            for(int j = i+1; j < m_tspBestPath.m_path.length; ++j)
            {
                testArray = new int[m_tspBestPath.m_path.length];
                System.arraycopy(m_tspBestPath.m_path,0,testArray,0,m_tspBestPath.m_path.length);

                //SWAP I and J
                swap(testArray,i,j);

                int thisEstimate = HeuristicSolver.getCost(testArray, m_graph, m_game);
                //System.out.println(i + " <-> " + j + ": " + thisEstimate);

                Integer[] newArray = new Integer[testArray.length];
                int index = 0;
                for (int value : testArray) {
                    newArray[index++] = Integer.valueOf(value);
                }

                m_paths.put(thisEstimate, newArray);

                /*if(thisEstimate<bestEstimate)
                {
                    bestEstimate = thisEstimate;
                    System.arraycopy(testArray,0,bestRoutePhy,0,m_tspBestPath.m_path.length);
                    System.out.println(" New BestEstimate: " + bestEstimate);
                }  */
            }
        }


        //Opt-3
        for(int i = 0; i < m_tspBestPath.m_path.length-2; ++i)
        {
            for(int j = i+1; j < m_tspBestPath.m_path.length-1; ++j)
            {
                for(int k = j+1; k < m_tspBestPath.m_path.length; ++k)
                {
                    testArray = new int[m_tspBestPath.m_path.length];
                    System.arraycopy(m_tspBestPath.m_path,0,testArray,0,m_tspBestPath.m_path.length);

                    //SWAP I, J and K
                    swap3(testArray,i,j,k);

                    int thisEstimate = HeuristicSolver.getCost(testArray, m_graph, m_game);

                    Integer[] newArray = new Integer[testArray.length];
                    int index = 0;
                    for (int value : testArray) {
                        newArray[index++] = Integer.valueOf(value);
                    }

                    m_paths.put(thisEstimate, newArray);

                    //The second swap:
                    swap3(testArray,k,i,j);

                    thisEstimate = HeuristicSolver.getCost(testArray, m_graph, m_game);

                    newArray = new Integer[testArray.length];
                    index = 0;
                    for (int value : testArray) {
                        newArray[index++] = Integer.valueOf(value);
                    }

                    m_paths.put(thisEstimate, newArray);
                }
            }
        }

    }

    public void showSubopt()
    {
        NavigableSet<Integer> set = m_paths.navigableKeySet();
        for(Integer key : set)
        {
            Integer[] path = m_paths.get(key);
            for(int i = 0; i < path.length; ++i)
                System.out.print(path[i] + " ");
            System.out.println(": " + key);
        }
    }


    public int[] getBestPath()
    {
        return m_tspBestPath.m_path;
    }

    public void assingBestPath(String a_bestPath)
    {
        String[] waypoints = a_bestPath.split(",");
        int[] wInt = new int[waypoints.length];
        for(int i = 0; i < waypoints.length; ++i)
            wInt[i] = Integer.parseInt(waypoints[i]);

        m_tspBestPath = new TSPPath(waypoints.length, wInt, 0);
    }


    private class TSPPath
    {
        public int m_nNodes;
        public double m_totalCost;
        public int[] m_path;

        public double m_speed;
        public int ticks;
        public Vector2d m_lastVector;

        public TSPPath(int a_nNodes, int[] a_nodes, double a_totCost)
        {
            m_lastVector = null;
            m_path = new int[a_nodes.length];
            m_nNodes = a_nNodes;
            m_totalCost =a_totCost;
            m_speed = 0;
            ticks = 0;
            System.arraycopy(a_nodes, 0, m_path, 0, a_nNodes);
        }


        public TSPPath getCopy()
        {
            TSPPath copied = new TSPPath(m_nNodes,m_path,this.m_totalCost);
            copied.m_speed = this.m_speed;
            copied.ticks = this.ticks;

            if(this.m_lastVector!=null)
                copied.m_lastVector = this.m_lastVector.copy();

            return copied;
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
