package framework.graph;

import java.awt.*;

/**
 * This class represents and edge of the graph.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class Edge
{
    /**
     * ID of the edge.
     */
    private long m_id;

    /**
     * ID of one of the nodes that this edge connects.
     */
    private int m_aId;

    /**
     * ID of one of the nodes that this edge connects.
     */
    private int m_bId;

    /**
     * Cost of this edge.
     */
    private double m_cost;

    /**
     * Constructor, receiving edge id, id of both nodes connected and cost.
     * @param a_id  ID of the edge.
     * @param a_aId ID of one of the nodes connected to this edge.
     * @param a_bId ID of the other node connected to this edge.
     * @param a_cost Cost of going from node a_aId to a_bId.
     */
    public Edge(long a_id, int a_aId, int a_bId, double a_cost)
    {
        m_id = a_id;
        m_aId = a_aId;
        m_bId = a_bId;
        m_cost = a_cost;
    }

    /**
     *  Draws this edge on the screen.
     *  @param a_graph Graph this edge belongs to.
     *  @param g Graphics device.
     */
    public void draw(Graph a_graph, Graphics2D g)
    {
        Node a = a_graph.getNode(m_aId);
        Node b = a_graph.getNode(m_bId);
        g.drawLine(a.x(), a.y(), b.x(), b.y());
    }

    /**
     * Gets the ID of the edge.
     * @return the ID of the edge.
     */
    public long id() {return m_id;}

    /**
     * Gets the ID of one of the nodes this edge connects
     * @return the ID of the node.
     */
    public int aId() {return m_aId;}

    /**
     * Gets the ID of one of the nodes this edge connects
     * @return the ID of the node.
     */
    public int bId() {return m_bId;}

    /**
     * Gets the cost of the edge.
     * @return the cost of the edge.
     */
    public double cost() {return m_cost;}

}