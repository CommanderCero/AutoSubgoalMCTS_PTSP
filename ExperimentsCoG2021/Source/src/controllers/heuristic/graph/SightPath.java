package controllers.heuristic.graph;

import controllers.mcts.ARCCOS;
import framework.core.Game;
import framework.utils.Vector2d;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 03/04/12
 * Time: 10:07
 * To change this template use File | Settings | File Templates.
 */
public class SightPath
{
    public Path p;
    public ArrayList<Integer> midPoints;
    public double angleSum;
    public Vector2d first;
    public Vector2d last;

    //public ArrayList<Vector2d> midVectors;
    public ArrayList<Double> midDistances;
    public ArrayList<Double> midDots;    //midDots[0] is dot between midVectors[0] and midVectors[1]
                                         //midDots[1] is dot between midVectors[1] and midVectors[2]

    public SightPath(Path a_p, Graph a_graph, Game a_game)
    {
        p = a_p;
        midPoints = new ArrayList<Integer>();
        midDistances = new ArrayList<Double>();
        //midVectors = new ArrayList<Vector2d>();
        midDots = new ArrayList<Double>();
        int numPoints = a_p.m_points.size();
        angleSum = 0;

        divide(0, numPoints-1, a_graph,a_game);

        Collections.sort(midPoints);

        if(midPoints.size() > 0)
        {
            Node org = a_graph.getNode(p.m_points.get(0));
            Node n0 = a_graph.getNode(p.m_points.get(midPoints.get(0)));

            Vector2d a = new Vector2d(n0.x() - org.x(), n0.y() - org.y());
            midDistances.add(a.mag());
            a.normalise();
            first = a.copy();
            //midVectors.add(a.copy());

            for(int i = 1; i<=midPoints.size(); ++i)
            {
                int p0 = midPoints.get(i-1);
                n0 = a_graph.getNode(p.m_points.get(p0));

                int p1;
                Node n1;
                if(i == midPoints.size())
                {
                    p1 = p.m_points.size()-1;
                    n1 = a_graph.getNode(p.m_points.get(p1));
                }else
                {
                    p1 = midPoints.get(i);
                    n1 = a_graph.getNode(p.m_points.get(p1));
                }

                Vector2d b = new Vector2d(n1.x() - n0.x(), n1.y() - n0.y());
                if(b.mag() > 0)
                {
                    midDistances.add(b.mag());
                    b.normalise();
                    last = b.copy();

                    double dot = a.dot(b);

                    //midVectors.add(b.copy());
                    midDots.add(dot);

                    angleSum += getAngleFromDot(dot);// a.angle(b);
                    a=b;
                }
            }

        }
        else
        {
            Node a = a_graph.getNode(p.m_points.get(0));
            Node b = a_graph.getNode(p.m_points.get(p.m_points.size()-1));
            first = new Vector2d(b.x() - a.x(), b.y() - a.y());
            midDistances.add(first.mag());
            first.normalise();
            last = first.copy();
            //midVectors.add(first.copy());
            //new Vector2d(a.x() - b.x(), a.y() - b.y());
            //last.normalise();
        }
    }


    public SightPath (SightPath a_inverse)
    {
        p = new Path(a_inverse.p, true);
        midPoints = a_inverse.getInvOrder();
        angleSum = a_inverse.angleSum;

        if(midPoints.size() == 0)
        {
            first = a_inverse.first.copy();
            first.mul(-1);
            last = first.copy();
        }else
        {
            first = a_inverse.last.copy();
            first.mul(-1);
            last = a_inverse.first.copy();
            last.mul(-1);
        }
    }


    public double getAngleFromDot(double dot)
    {
        double angle = ARCCOS.getArcos(dot); //Math.acos(dot);
        return angle;
    }

    public double getAngle(Vector2d a, Vector2d b)
    {
        
        double dot = a.dot(b);
        double angle = ARCCOS.getArcos(dot); //Math.acos(dot);
        return angle;
    }

    public ArrayList<Integer> getOrder() {return midPoints;}

    private ArrayList<Integer> getInvOrder()
    {
        /*ArrayList<Integer> inv = new ArrayList<Integer>();
        for(int i = midPoints.size()-1; i >= 0; --i)
            inv.add(midPoints.get(i));
        return inv;  */

        ArrayList<Integer> inv = new ArrayList<Integer>();
        int numPoints = p.m_points.size();
        for(int i = midPoints.size()-1; i >= 0; --i)
        {
            int thisPoint = numPoints - midPoints.get(i);
            inv.add(thisPoint);
        }
        return inv;
    }

    /*private void divide(int or, int dest, Graph a_graph, Game a_game)
    {
        int midPoint = or + ((dest-or)/2);
       // Vector2d mid = new Vector2d(a_graph.getNode(p.m_points.get(midPoint)).x(),a_graph.getNode(p.m_points.get(midPoint)).y());
        midPoints.add(midPoint);

    } */

    private void divide(int or, int dest, Graph a_graph, Game a_game)
    {
        Vector2d orV = new Vector2d(a_graph.getNode(p.m_points.get(or)).x(),a_graph.getNode(p.m_points.get(or)).y());
        Vector2d destV = new Vector2d(a_graph.getNode(p.m_points.get(dest)).x(),a_graph.getNode(p.m_points.get(dest)).y());

        if(a_game.getMap().LineOfSight(orV,destV))
        {
            return;
        }else
        {
            int midPoint = or + ((dest-or)/2);
            Vector2d mid = new Vector2d(a_graph.getNode(p.m_points.get(midPoint)).x(),a_graph.getNode(p.m_points.get(midPoint)).y());

            //or -> mid
            double distA = mid.dist(orV);
            if(distA > 5)
            {
                if(a_game.getMap().LineOfSight(orV,mid))
                {
                    midPoints.add(midPoint);
                }else
                {
                    //System.out.println("DIVIDING: " + or + " TO " + midPoint);
                    divide(or,midPoint,a_graph,a_game);

                }
            }

            //dest -> mid
            double distB = mid.dist(destV);
            if(distB > 5)
            {
                if(a_game.getMap().LineOfSight(destV,mid))
                {
                    midPoints.add(dest);
                }else
                {
                    //System.out.println("DIVIDING: " + midPoint + " TO " + dest);
                    divide(midPoint,dest,a_graph,a_game);
                }
            }

        }
    }
}
