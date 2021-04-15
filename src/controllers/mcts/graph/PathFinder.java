package controllers.mcts.graph;

import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Class for pathfinding. This class may be used by the controllers in order to navigate through the map. This class uses
 * a grid-based graph created over the navigable parts of the map.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class PathFinder
{
    /**
     * Graph to be used for the path finding.
     */
    private Graph m_graph;

    /**
     * Cache of shortest path from node i (index of map) to the others
     */
    private HashMap<Integer, HashMap<Integer, Path>> m_shortestPaths;

    /**
     * Constructor of the pathfinder.
     * @param a_graph Graph of the game.
     */
    public PathFinder(Graph a_graph)
    {
        m_graph = a_graph;
        m_shortestPaths = new HashMap<Integer, HashMap<Integer, Path>>();

        Object []nodes = m_graph.getNodesArray();
        int numNodes = nodes.length;

        //Init paths
        for(int i = 0; i < numNodes; ++i)
        {
            Node n = (Node)nodes[i];
            initShortestPaths(n);
        }
    }

    /**
     * Initializes the cache of shortest paths from a given node.
     * @param a_origin node the paths start in.
     */
    private void initShortestPaths(Node a_origin)
    {
        for(long edgeId:a_origin.getEdgesFromNode())
        {
            Edge edg = m_graph.getEdge(edgeId);
            int connectedID = edg.aId() != a_origin.id() ? edg.aId() : edg.bId();
            assignCost(a_origin.id(), connectedID, edg.cost());
        }

        //Default one, to itself
        assignCost(a_origin.id(), a_origin.id(), 0);
    }

    /**
     * Assigns a cost to a path between origin and destination.
     * @param a_originID Origin node id.
     * @param a_destID Destination node id.
     * @param a_cost Cost from a_originID to a_destID
     */
    private void assignCost(int a_originID, int a_destID, double a_cost)
    {
        //1st, get the origin:
        HashMap<Integer, Path> originPaths = m_shortestPaths.get(a_originID);
        if(originPaths == null)
        {
             originPaths = new HashMap<Integer, Path>();

             //There will be no destination, for sure, so lets assign it.
             Path newPath = new Path(a_originID, a_destID, a_cost);
             originPaths.put(a_destID, newPath);

             //To the array!
             m_shortestPaths.put(a_originID, originPaths);

             //And that's all
             return;
        }

        //2nd, destination
        Path pathToDest = originPaths.get(a_destID);
        if(pathToDest == null)
        {
            //no path, create it and insert.
            Path newPath = new Path(a_originID, a_destID, a_cost);
            originPaths.put(a_destID, newPath);

            //And that's all
            return;
        }

        //3rd, there is a path, no cost; assign.
        pathToDest.m_cost = a_cost;
    }


    /**
     * Gets the shortest path from origin to destination, stored in the shortestPath cache. Returns null if no path can be found.
     * @param a_origin origin node id.
     * @param a_destination destination node id.
     * @return Path from origin to destination, null if not found.
     */
    private Path getShortestPath(int a_origin, int a_destination)
    {
        Path p = m_shortestPaths.get(a_origin).get(a_destination);
        if(p == null)
            return new Path(a_origin, a_destination, Double.MAX_VALUE);
        return p;
    }

    /**
     * Includes the given path in the shortest paths cache.
     * @param a_p Path to include.
     */
    private void setShortestPath(Path a_p)
    {
        HashMap<Integer, Path> shortest = getShortestPaths(a_p.m_originID);
        shortest.put(a_p.m_destinationID, a_p);
    }

    /**
     * Gets a path between two nodes in the graph.  It checks the cache of shortest paths to see if it was calculated before.
     * @param a_origin origin node id.
     * @param a_destination destination node id.
     * @return the path from a_origin to a_destination.
     */
    public Path getPath(int a_origin, int a_destination)
    {
        Path shortestP = getShortestPath(a_origin, a_destination);
        if(shortestP != null && shortestP.m_cost != Double.MAX_VALUE)
        {
            //The path was there.
            return shortestP;
        }

        //No path, need to calculate with A*
        Path p = new Path(a_origin, a_destination);
        Path empty = new Path(a_origin, a_origin);

        boolean pathFound = _a_star(p);

        if(!pathFound)
        {
            //If path could not be found, clear it
            p.m_points.clear();
        }
        else
        {
            //Else, extract the path
            p = getShortestPath(a_origin,a_destination);
        }

        //Return the path if it is meaningful.
        if (p.m_points.size() > 0 && p.m_cost < Integer.MAX_VALUE)
            return p;
        else return empty;
    }

    /**
     * A star method to calculate the shortest path between the nodes a_path.m_originID and a_path.m_destinationID
     * @param a_path Path to fill with intermediate nodes.
     * @return true if the path could be found.
     */
    private boolean _a_star(Path a_path)
    {
        //Sets of evaluated an not evaluated nodes.
        LinkedList<Node> evaluatedSet = new LinkedList<Node>();
        PriorityQueue<Node> toEvaluateSet = new PriorityQueue<Node>(1000,new NodeComparatorH());

        //Initialize current node (origin).
        Node currentNode = m_graph.getNode(a_path.m_originID);
        toEvaluateSet.add(currentNode);
        currentNode.m_g = 0;
        currentNode.m_h = heuristic(a_path.m_originID, a_path.m_destinationID);
        currentNode.m_f = currentNode.m_g + currentNode.m_h;

        //Check while there are still nodes in the array of nods to evaluate.
        while(!toEvaluateSet.isEmpty())
        {
            //Take next node to evaluate.
            currentNode = toEvaluateSet.poll();
            int currentNodeId = currentNode.id();
            evaluatedSet.add(currentNode);

            //If destination found, that's it.
            if(currentNode.id() == a_path.m_destinationID)
            {
                return true;
            }

            //For all edges from the current node...
            for(Long edgeId:currentNode.getEdgesFromNode())
            {
                //Take the edge.
                Edge edg = m_graph.getEdge(edgeId);
                int connectedID = edg.aId() != currentNodeId ? edg.aId() : edg.bId();
                Node connected = m_graph.getNode(connectedID);

                //If it has not been evaluated yet.
                if(!evaluatedSet.contains(connected))
                {
                    // Cost from origin to 'connected' stored
                    Path D1 = getShortestPath(a_path.m_originID,connectedID);
                    // Cost from origin to current node stored
                    Path DA = getShortestPath(a_path.m_originID,currentNodeId);
                    // Cost from current to connected (edge cost)
                    double dA1 = edg.cost();

                    //Path to this node
                    PathCH pc = new PathCH();
                    pc.p = D1;
                    pc.destID = connectedID;

                    //If the new cost is smaller.
                    double newCost = DA.m_cost + dA1;
                    if(D1.m_cost > newCost)
                    {
                        //update cost
                        Path newD1 = new Path(DA);
                        newD1.m_destinationID = connectedID;
                        newD1.m_cost += dA1;
                        //update path
                        pc.p = newD1;
                        newD1.m_points.add(connectedID);
                        setShortestPath(newD1);
                    }

                    //Set cost, used by priority queue to navigate more efficiently
                    connected.m_g = pc.p.m_cost;
                    connected.m_f = pc.heuristicCost = pc.p.m_cost + heuristic(connectedID, a_path.m_destinationID);
                    if(!toEvaluateSet.contains(connected))
                    {
                        //Mark this node as evaluated.
                        toEvaluateSet.add(connected);
                    }

                }
            }
        }

        //If we didn't find the destination, the path could not be extracted.
        return false;
    }

    /**
     * Heuristic for A*: euclidean distance.
     * @param a_or origin node id.
     * @param a_dest destination node id.
     * @return the euclidean distance between origin and destination.
     */
    private double heuristic(int a_or, int a_dest)
    {
        return m_graph.getNodes().get(a_or).euclideanDistanceTo(m_graph.getNodes().get(a_dest));
    }

    /**
     * Returns all the shortest paths in cache from a given node.
     * @param a_origin origin node id.
     * @return a HashMap with all the paths from the origin node.
     */
    private HashMap<Integer, Path> getShortestPaths(int a_origin){return m_shortestPaths.get(a_origin);}

}

/**
 * CLASS to compare two nodes by the graph cost.
 */
class NodeComparatorG implements Comparator<Node>
{
    /**
     * Compares two nodes.
     * @param x one node.
     * @param y another node.
     * @return returns -1, 0, or 1 depending on the nodes: -1 if x has a smaller graph cost than y, 1 if opposite, 0 if it is the same cost.
     */
    public int compare(Node x, Node y)
    {
        // Assume neither PathCH is null. Real code should
        // probably be more robust
        if (x.m_g < y.m_g)
        {
            return -1;
        }
        if (x.m_g > y.m_g)
        {
            return 1;
        }
        return 0;
    }
}


/**
 * CLASS to compare two nodes by the heuristic cost.
 */
class NodeComparatorH implements Comparator<Node>
{
    /**
     * Compares two nodes.
     * @param x one node.
     * @param y another node.
     * @return returns -1, 0, or 1 depending on the nodes: -1 if x has a smaller heuristic cost than y, 1 if opposite, 0 if it is the same cost.
     */
    public int compare(Node x, Node y)
    {
        // Assume neither PathCH is null. Real code should
        // probably be more robust
        if (x.m_f < y.m_f)
        {
            return -1;
        }
        if (x.m_f > y.m_f)
        {
            return 1;
        }
        return 0;
    }
}

/**
 * CLASS to compare two paths by the heuristic cost.
 */
class PathCHComparator implements Comparator<PathCH>
{
    /**
     * Compares two paths.
     * @param x one path.
     * @param y another path.
     * @return returns -1, 0, or 1 depending on the pat:hs -1 if x has a smaller heuristic cost than y, 1 if opposite, 0 if it is the same cost.
     */
    public int compare(PathCH x, PathCH y)
    {
        // Assume neither PathCH is null. Real code should
        // probably be more robust
        if (x.heuristicCost < y.heuristicCost)
        {
            return -1;
        }
        if (x.heuristicCost > y.heuristicCost)
        {
            return 1;
        }
        return 0;
    }
}

/**
 * Helper CLASS used y A* to compare paths.
 */
class PathCH
{
    /**
     * Path
     */
    public Path p;

    /**
     * Destination node.
     */
    public int destID;

    /**
     * Heuristic cost
     */
    public double heuristicCost;

    /**
     * Overrides toString() [debug purposes].
     * @return A string representation of the path
     */
    @Override
    public String toString() {return "[" + p.toString() + "] hC: " + heuristicCost; }
}
