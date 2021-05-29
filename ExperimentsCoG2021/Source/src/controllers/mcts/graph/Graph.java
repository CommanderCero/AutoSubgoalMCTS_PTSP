package controllers.mcts.graph;

import framework.core.Game;
import framework.core.Map;
import framework.utils.Vector2d;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class represents the navigable graph of the map.The controller may use this class to query for shortest
 * path distances in the map that is beng played.To create the graph, the controller has to use the constructor provided.
 * To query for paths, the controller has to use the function getPath, providing IDs of the nodes to check. The IDs of the nodes
 * can be retrieved, by supplying the position in the map desired, by usign the function getClosestNodeTo().
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class Graph
{
    /**
     * Granularity of the graph (distance between two consecutive nodes, in pixels).
     */
    public final int GRANULARITY = 8;

    /**
     * Cost of an edge that links two not diagonally adjacent nodes.
     */
    public final double COST_STRAIGHT = GRANULARITY;

    /**
     * Cost of an edge that links two diagonally adjacent nodes.
     */
    public final double COST_DIAG = Math.sqrt(GRANULARITY*GRANULARITY + GRANULARITY*GRANULARITY);

    /**
     * Edges of the graph
     */
    private HashMap<Long, Edge> m_edges;

    /**
     * Nodes of the graph.
     */
    private HashMap<Integer, Node> m_nodes;

    /**
     * PathFinder class to do... path finding!
     */
    PathFinder m_pathFinder;

    /**
     * Map where the game is being played in.
     */
    private Map m_map;

    /**
     * Constructor of the graph.
     * @param a_game Game reference.
     */
    public Graph(Game a_game)
    {
        //Initialise some values.
        m_map = a_game.getMap();
        m_edges = new HashMap<Long, Edge>();
        m_nodes = new HashMap<Integer, Node>();
        int distance = 5;

        //Create the nodes:
        for(int i = 0; i < m_map.getMapChar().length; i+=GRANULARITY)
        {
            for(int j = 0; j < m_map.getMapChar()[0].length; j+=GRANULARITY)
            {
                if(!m_map.isObstacle(i,j) && !isObstacleClose(i, j, distance) && !a_game.getShip().checkCollisionInPosition(new Vector2d(i,j)))
                {
                    addNode(i,j);
                }
            }
        }

        //Create the links between nodes:
        Collection<Node> nodes = m_nodes.values();
        for(Node n: nodes)
        {
            eightWayConnectivity(n);
        }

        //Init the PathFinder when the graph is created.
        m_pathFinder = new PathFinder(this);
    }

    /**
     * Returns true if there exists an obstacle close to the given position, where "close" is defined by a param.
     * @param x x position in the map.
     * @param y y position in the map.
     * @param maxDist maximum distance to check.
     * @return true if an obstacle exists.
     */
    private boolean isObstacleClose(int x, int y, int maxDist)
    {
        for(int dist = 1; dist <= maxDist; ++dist)
        {
            //up
            if(y-dist >=0 && m_map.isObstacle(x,y-dist)) return true;
            //down
            if(y+dist < m_map.getMapHeight() && m_map.isObstacle(x,y+dist)) return true;
            //left
            if(x-dist >=0 && m_map.isObstacle(x-dist,y)) return true;
            //right
            if(x+dist < m_map.getMapWidth() && m_map.isObstacle(x+dist,y)) return true;
            //up-left
            if(y-dist >=0 && x-dist >=0 && m_map.isObstacle(x-dist,y-dist)) return true;
            //up-right
            if(y-dist >=0 && x+dist < m_map.getMapWidth() && m_map.isObstacle(x+dist,y-dist)) return true;
            //down-left
            if(y+dist < m_map.getMapHeight() && x-dist >=0 && m_map.isObstacle(x-dist,y+dist)) return true;
            //down-right
            if(y+dist < m_map.getMapHeight() && x+dist < m_map.getMapWidth() && m_map.isObstacle(x+dist,y+dist)) return true;

        }
        //else
        return false;
    }

    /**
     * Links the node given with all the neighbours.
     * @param n the node to link
     */
    private void eightWayConnectivity(Node n)
    {
        int x = n.x();
        int y = n.y();

        //up
        if(y-GRANULARITY >=0)
        {
            int nodeID = existsNode(x,y-GRANULARITY);
            if(nodeID != -1 && m_map.checkObsFree(x, y, x, y-GRANULARITY))
                addEdge(n.id(), nodeID, COST_STRAIGHT);


            //up+left
            if(x-GRANULARITY >=0)
            {
                nodeID = existsNode(x - GRANULARITY, y - GRANULARITY);
                if(nodeID != -1 && m_map.checkObsFree(x, y, x- GRANULARITY, y-GRANULARITY))
                    addEdge(n.id(), nodeID, COST_DIAG);
            }

            //up+right
            if(x+GRANULARITY < m_map.getMapWidth())
            {
                nodeID = existsNode(x + GRANULARITY, y - GRANULARITY);
                if(nodeID != -1 && m_map.checkObsFree(x, y, x + GRANULARITY, y - GRANULARITY))
                    addEdge(n.id(), nodeID, COST_DIAG);
            }

        }

        //down
        if(y+GRANULARITY < m_map.getMapHeight())
        {
            int nodeID = existsNode(x, y + GRANULARITY);
            if(nodeID != -1 && m_map.checkObsFree(x, y, x, y + GRANULARITY))
                addEdge(n.id(), nodeID, COST_STRAIGHT);


            //down+left
            if(x-GRANULARITY >=0)
            {
                nodeID = existsNode(x - GRANULARITY, y + GRANULARITY);
                if(nodeID != -1 && m_map.checkObsFree(x, y, x - GRANULARITY, y + GRANULARITY))
                    addEdge(n.id(), nodeID, COST_DIAG);
            }

            //down+right
            if(x+GRANULARITY < m_map.getMapWidth())
            {
                nodeID = existsNode(x + GRANULARITY, y + GRANULARITY);
                if(nodeID != -1 && m_map.checkObsFree(x, y, x + GRANULARITY, y + GRANULARITY))
                    addEdge(n.id(), nodeID, COST_DIAG);
            }

        }

        //left
        if(x-GRANULARITY >=0)
        {
            int nodeID = existsNode(x-GRANULARITY,y);
            if(nodeID != -1 && m_map.checkObsFree(x, y, x-GRANULARITY, y))
                addEdge(n.id(), nodeID, COST_STRAIGHT);
        }

        //right
        if(x+GRANULARITY < m_map.getMapWidth())
        {
            int nodeID = existsNode(x + GRANULARITY, y);
            if(nodeID != -1 && m_map.checkObsFree(x, y, x + GRANULARITY, y ))
                addEdge(n.id(), nodeID, COST_STRAIGHT);
        }

    }

    /**
     * Adds a new node to the graph in the indicated position.
     * @param a_x x position in the map.
     * @param a_y y position in the map.
     * @return the id of the new node.
     */
    private int addNode(int a_x, int a_y)
    {
        //calculate an unique ID depending on the coordinates:
        int id = 100000*(100+a_y) + (10000+a_x);
        Integer idKey = id;

        if(!m_nodes.containsKey(idKey))
        {
            //Not registered, create node and insert it.
            Node newNode = new Node(id, a_x, a_y);
            m_nodes.put(idKey, newNode);
        }

        return id;
    }

    /**
     * Checks if there is an existing node in the given coordinates.
     * @param a_x x coordinate.
     * @param a_y y coordinate.
     * @return If it does not exist, returns -1. Otherwise, return its ID.
     */
    private int existsNode(int a_x, int a_y)
    {
    //calculate an unique ID depending on the coordinates:
        int id = 100000*(100+a_y) + (10000+a_x);
        Integer idKey = id;

        if(!m_nodes.containsKey(idKey))
            return -1;
        else return id;
    }

    /**
     * Checks if there is an existing node with the given ID.
     * @param a_id id of the node of check.
     * @return If it does not exist, returns -1. Otherwise, return its ID.
     */
    private int existsNode(int a_id)
    {
        if(!m_nodes.containsKey(a_id))
            return -1;
        else return a_id;
    }

    /**
     * Adds an edge to the graph.
     * @param a_aID Id of one of the nodes for this edge.
     * @param a_bID Id of the other node for this edge.
     * @param a_cost Cost of the new edge.
     */
    private void addEdge(int a_aID, int a_bID, double a_cost)
    {
        //calculate an unique ID depending on the IDs of the nodes:
        long id = (100000000L * a_aID) + a_bID;
        Long idKey = id;

        if(!m_edges.containsKey(idKey))
        {
            //Not registered, create node and insert it.
            //Get the nodes of the edge
            Node nodeA = m_nodes.get(a_aID);
            Node nodeB = m_nodes.get(a_bID);

            if(nodeA != null && nodeB != null)
            {
                Edge newEdgeAB = new Edge(id, a_aID, a_bID, a_cost);
                m_edges.put(idKey, newEdgeAB);
                nodeA.addEdge(id);

                long id2 = (100000000L * a_bID) + a_aID;
                Long idKey2 = id2;
                Edge newEdgeBA = new Edge(id2, a_bID, a_aID, a_cost);
                m_edges.put(idKey2, newEdgeBA);
                nodeB.addEdge(id2);

            }
        }
    }

    /**
     * Checks if there is an existing edge between two nodes.
     * @param a_aID Id of one of the nodes.
     * @param a_bID Id of the other node.
     * @return If it does not exist, returns -1. Otherwise, return its ID.
     */
    private long existsEdge(int a_aID, int a_bID)
    {
        long id = (100000000L * a_aID) + a_bID;
        Long idKey = id;

        if(!m_edges.containsKey(idKey))
        {
            return -1;
        }else return id;
    }

    /**
     * Gets and edge betwwen two nodes.
     * @param a_aID Id of one of the nodes.
     * @param a_bID Id of the other node.
     * @return The edge that may exist. Return null if it does not exist.
     */
    private Edge getEdge(int a_aID, int a_bID)
    {
        //calculate an unique ID depending on the nodes:
        long id = (100000000L * a_aID) + a_bID;
        Long idKey = id;

        if(!m_edges.containsKey(idKey))
        {
            //No edge
            return null;
        }

        return m_edges.get(idKey);
    }

    /**
     * Gets and edge between two nodes.
     * @param a_ID Id of the edge we are looking for.
     * @return the edge, or null if it does not exist.
     */
    public Edge getEdge(long a_ID)
    {
        if(!m_edges.containsKey(a_ID))
        {
            //No edge
            return null;
        }

        return m_edges.get(a_ID);
    }

    /**
     * Gets the closest node in the map to the position given. We can specify if we want to check for obstacles between the
     * position supplied and the node looked for. It MAY BE POSSIBLE that there is no node with no obstacles in between.
     * @param a_x x position in the map.
     * @param a_y y position in the map.
     * @param a_checkObsFree If true, the method returns the closest node to the given position checking as well that there is
     *                       no obstacle between the position and the node. Otherwise, obstacles are not checked.
     * @return The node found, null if no node was found.
     */
    public Node getClosestNodeTo(double a_x, double a_y, boolean a_checkObsFree)
    {
        int xPos = (int)Math.round(a_x);
        int yPos = (int)Math.round(a_y);

        int factor = 2;
        int startX = xPos - GRANULARITY*factor, startY = yPos - GRANULARITY*factor;
        int endX = xPos + GRANULARITY*factor, endY = yPos + GRANULARITY*factor;
        Node bestNode = getClosestNodeIn(a_x,a_y,startX,startY,endX,endY,a_checkObsFree);
        while(bestNode == null)
        {
            if(factor > 20)
            {
               /* System.out.println("Ups! Sorry, something went terribly wrong with the graph." +
                        " A node close to the position given (" + a_x + "," + a_y + ") couldn't" +
                        " be found. Please, check if you are not trying to reach a non-accessible point in the map.");*/
                return null;
            }

            factor++;
            startX = xPos - GRANULARITY*factor; startY = yPos - GRANULARITY*factor;
            endX = xPos + GRANULARITY*factor; endY = yPos + GRANULARITY*factor;
            bestNode = getClosestNodeIn(a_x,a_y,startX,startY,endX,endY,a_checkObsFree);
        }

        return bestNode;
    }

    /**
     * Gets the closest node in the map to the position given. We don't specify if we want to check for obstacles between the
     * position supplied and the node looked for. In this case, it checks considering the obstacles. If no node is found in the near
     * positions of the point, it checks without considering collisions.
     * @param a_x x position in the map.
     * @param a_y y position in the map.
     * @return The node found, null if no node was found.
     */
    public Node getClosestNodeTo(double a_x, double a_y)
    {
        int xPos = (int)Math.round(a_x);
        int yPos = (int)Math.round(a_y);
        boolean checkObstacles = true;

        int factor = 2;
        int startX = xPos - GRANULARITY*factor, startY = yPos - GRANULARITY*factor;
        int endX = xPos + GRANULARITY*factor, endY = yPos + GRANULARITY*factor;
        Node bestNode = getClosestNodeIn(a_x,a_y,startX,startY,endX,endY,checkObstacles);

        while(bestNode == null)
        {
            if(factor > 10)
            {
                break;
            }

            factor++;
            startX = xPos - GRANULARITY*factor; startY = yPos - GRANULARITY*factor;
            endX = xPos + GRANULARITY*factor; endY = yPos + GRANULARITY*factor;
            bestNode = getClosestNodeIn(a_x,a_y,startX,startY,endX,endY,checkObstacles);
        }

        if(bestNode == null)
            bestNode = getClosestNodeTo(a_x, a_y, false);

        return bestNode;


    }



    /**
     * Gets the closest node to a position, from a given position, checking ship radius if desired.
     * @param a_x x position to checck.
     * @param a_y y position to check.
     * @param a_startX x position to start looking from.
     * @param a_startY y position to start looking from.
     * @param a_endX x position to end the search.
     * @param a_endY y position to end the search.
     * @param a_checkObsFree If true, the method returns the closest node to the given position checking as well that there is
     *                       no obstacle between the position and the node. Otherwise, obstacles are not checked.
     * @return The node looked for, or null if not found.
     */
    private Node getClosestNodeIn(double a_x, double a_y, int a_startX, int a_startY, int a_endX, int a_endY, boolean a_checkObsFree)
    {
        Node bestNode = null;
        int xPos = (int)Math.round(a_x);
        int yPos = (int)Math.round(a_y);
        double bestDistance = Double.MAX_VALUE;

        for(int x = a_startX; x <= a_endX; ++x)
        {
            for(int y = a_startY; y <= a_endY; ++y)
            {
                if(x >= 0 && y >= 0 && x < m_map.getMapWidth() && y < m_map.getMapHeight())
                {
                    int nodeID = existsNode(x, y);
                    if(nodeID != -1)
                    {
                        Node n = getNode(nodeID);
                        double distance = n.euclideanDistanceTo(a_x,a_y);
                        if(distance < bestDistance)
                        {
                            if(a_checkObsFree)
                            {
                                if(m_map.checkObsFree(xPos, yPos, n.x(), n.y()))
                                {
                                    bestNode = n;
                                    bestDistance = distance;
                                }
                            }else
                            {
                                bestNode = n;
                                bestDistance = distance;
                            }
                        }
                    }
                }
            }
        }

        return bestNode;
    }

    /**
     * Gets a path from origin to destination. If it was calculated before, it returns it from the cache
     * @param a_origin ID of the origin node
     * @param a_destination ID of the destination node.
     * @return the path from a_origin to a_destination
     */
    public Path getPath(int a_origin, int a_destination)
    {
        return m_pathFinder.getPath(a_origin, a_destination);
    }

    /**
     * Draws the graph on the screen.
     * @param g Graphics device to draw.
     */
    public void draw(Graphics2D g)
    {
        Collection<Node> nodes = m_nodes.values();
        for(Node n: nodes)
        {
            n.draw(this, g);
        }
    }

    /**
     * Gets an array with all the nodes of the graph.
     * @return an array with all the nodes of the graph.
     */
    public Object[] getNodesArray() { return m_nodes.values().toArray();}

    /**
     * Returns the cache of nodes, indexed by their IDs.
     * @return the cache of nodes.
     */
    public HashMap<Integer, Node> getNodes(){return m_nodes; }

    /**
     * Gets the edges of the graph, indexed by their IDs.
     * @return the edges of the graph.
     */
    public HashMap<Long, Edge> getEdges() { return m_edges; }

    /**
     * Gets the node that corresponds to the given ID.
     * @param a_id ID of the node.
     * @return the node, null if not found by that ID.
     */
    public Node getNode(int a_id) { return m_nodes.get(a_id); }

    /**
     * Gets the number of nodes of the graph.
     * @return the number of nodes of the graph.
     */
    public int getNumNodes() {return m_nodes.size();}

}
