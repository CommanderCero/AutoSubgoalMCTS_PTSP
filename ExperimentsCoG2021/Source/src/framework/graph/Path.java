package framework.graph;

import java.util.Vector;

/**
 * Represents a path from one node to another
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class Path
{
    /**
     * Identifier of the origin's node of the path
     */
    public int m_originID;

    /**
     * Identifier of the destination's node of the path
     */
    public int m_destinationID;

    /**
     * Cost of the complete path.
     */
    public double m_cost;

    /**
     * IDs of the nodes this path is formed by.
     */
    public Vector<Integer> m_points;

    /**
     * Creates an empty path from the origin to the end.
     * @param a_start origin node id of the path.
     * @param a_end destination node id of the path.
     */
    public Path(int a_start, int a_end)
    {
        m_originID = a_start;
        m_destinationID = a_end;
        m_cost = Integer.MAX_VALUE;
        m_points = new Vector<Integer>();

        //These two points MUST be in the path!!
        m_points.add(a_start);
        if(a_start != a_end) m_points.add(a_end);
    }

    /**
     * Creates an empty path from the origin to the end, giving a cost.
     * @param a_start origin node id of the path.
     * @param a_end destination node id of the path.
     * @param a_costP cost of the path.
     */
    public Path(int a_start, int a_end, double a_costP)
    {
        m_originID = a_start;
        m_destinationID = a_end;
        m_cost = a_costP;
        m_points = new Vector<Integer>();

        //These two points MUST be in the path!!
        m_points.add(a_start);
        if(a_start != a_end) m_points.add(a_end);
    }

    /**
     * Creates a path as a copy of another path.
     * @param a_p path to copy from.
     */
    public Path(Path a_p)
    {
        m_originID = a_p.m_originID;
        m_destinationID = a_p.m_destinationID;
        m_cost = a_p.m_cost;
        m_points = new Vector<Integer>();
        for(int i = 0; i < a_p.m_points.size(); ++i)
        {
            m_points.add(a_p.m_points.get(i));
	    }
    }

}

