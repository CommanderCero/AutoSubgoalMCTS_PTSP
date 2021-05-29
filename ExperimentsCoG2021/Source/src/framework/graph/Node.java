package framework.graph;

import java.awt.*;
import java.util.Vector;

/**
 * This class represents a node for paths and graph.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class Node implements Comparable
{
    /**
     * X-position of the node in the map.
     */
    private int m_x;

    /**
     * Y-position of the node in the map.
     */
    private int m_y;

    /**
     * Edges connected to this node.
     */
    private Vector<Long> m_edges;

    /**
     * ID of this node.
     */
    private int m_id;

    /**
     * Radius of the node (used only for debug drawing).
     */
    public final int RADIUS = 4;

    /**
     * Color of a node (used only for debug drawing).
     */
    public final Color nodeCol = new Color(128,0,128);

    /**
     * Color of the edges of the graph (used only for debug drawing).
     */
    public final Color edgeCol = new Color(128,128,255);

    /**
     * Graph cost of the node.
     */
    public double m_g;

    /**
     * Heuristic cost of the node.
     */
    public double m_h;

    /**
     * Total cost of the node.
     */
    public double m_f;

    /**
     * Constructor of the node, given position and id.
     * @param a_id id of the node.
     * @param a_x X-position of the node in the map.
     * @param a_y Y-position of the node in the map.
     */
    public Node(int a_id, int a_x, int a_y)
    {
        m_x = a_x;
        m_y = a_y;
        m_id = a_id;
        m_edges = new Vector<Long>();
        m_g = m_h = m_f = Double.MAX_VALUE;
    }

    /**
     * Draws the node on the screen.
     * @param a_graph Graph that this node belongs to.
     * @param g Graphics object to paint.
     */
    public void draw(Graph a_graph, Graphics2D g)
    {
        //edges of this node
        g.setColor(edgeCol);
        for(Long id:m_edges)
        {
            Edge e = a_graph.getEdge(id);
            e.draw(a_graph,g);
        }

        //the proper node.
        g.setColor(nodeCol);
        g.fillOval((int) (m_x-RADIUS*0.5), (int) (m_y-RADIUS*0.5), RADIUS, RADIUS);

    }

    /**
     * Manhattan distance from this node to another
     * @param a_other The other node.
     * @return the distance
     */
    public int manhattanDistanceTo(Node a_other)
    {
        return manhattanDistanceTo(a_other.x(),a_other.y());
    }

    /**
     * Manhattan distance from this node to a given position
     * @param a_x X value of the position in the map.
     * @param a_y Y value of the position in the map.
     * @return the distance
     */
    public int manhattanDistanceTo(int a_x, int a_y)
    {
        int xDiff = Math.abs(m_x-a_x);
        int yDiff = Math.abs(m_y-a_y);
        return xDiff + yDiff;
    }

    /**
     * Euclidean distance from this node to another
     * @param a_other The other node.
     * @return the distance
     */
    public double euclideanDistanceTo(Node a_other)
    {
        return euclideanDistanceTo(a_other.x(),a_other.y());
    }

    /**
     * Euclidean distance from this node to a given position
     * @param a_x X value of the position in the map.
     * @param a_y Y value of the position in the map.
     * @return the distance
     */
    public double euclideanDistanceTo(double a_x, double a_y)
    {
        double xDiff = m_x-a_x;
        double yDiff = m_y-a_y;
        return Math.sqrt(xDiff*xDiff + yDiff*yDiff);
    }

    /**
     * Compares this node with another. Returns true if positions are the same.
     * @param o the other object, that must be a node.
     * @return 0 if they are the same, -1 if they aren't
     */
    public int compareTo(Object o)
    {
        Node other = (Node)o;
        if(other.x() == m_x && other.y() == m_y)
            return 0;
        return -1;
    }

    /**
     * Gets the x-position of the node in the map.
     * @return the x-position of the node in the map.
     */
    public int x() {return m_x;}

    /**
     * Gets the y-position of the node in the map.
     * @return the y-position of the node in the map.
     */
    public int y() {return m_y;}

    /**
     * Gets the identifier of the node.
     * @return the identifier of the node.
     */
    public int id() {return m_id;}

    /**
     * Gets the edges connected to this node.
     * @return A vector of identifiers of the edges connected to this node.
     */
    public Vector<Long> getEdgesFromNode() {return m_edges;}

    /**
     * Adds a new edge to this node.
     * @param a_id Edge id.
     */
    public void addEdge(long a_id) {m_edges.add(a_id);}

    /**
     * Sets the x position of the node in the world.
     * @param a_x the x position of the node in the world.
     */
    public void x(int a_x) {m_x = a_x;}

    /**
     * Sets the y position of the node in the world.
     * @param a_y the y position of the node in the world.
     */
    public void y(int a_y) {m_y = a_y;}
}
