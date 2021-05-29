package controllers.lineofsight;

import framework.core.Controller;
import framework.core.Game;
import framework.core.Waypoint;
import framework.utils.Vector2d;

import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 07/03/12
 * Time: 11:53
 * To change this template use File | Settings | File Templates.
 */
public class LineOfSight extends Controller
{

    /**
     * Random number generator
     */
    private Random m_rnd;

    /**
     * Closest waypoint to the ship.
     */
    private Waypoint m_closestWaypoint;


    /**
     * Constructor, that receives a copy of the game state
     * @param a_gameCopy a copy of the game state
     */
    public LineOfSight(Game a_gameCopy, long a_timeDue)
    {
        m_rnd = new Random();
    }

    /**
     * This function is called every execution step to get the action to execute.
     * @param a_gameCopy Copy of the current game state.
     * @param a_timeDue The time the next move is due
     * @return the integer identifier of the action to execute (see interface framework.core.Controller for definitions)
     */
    public int getAction(Game a_gameCopy, long a_timeDue)
    {
        //Calculate the closest waypoint to the ship.
        calculateClosestWaypoint(a_gameCopy);

        //We check if there is a line of sight between the ship and the waypoint:
        boolean isThereLineOfSight = a_gameCopy.getMap().LineOfSight(a_gameCopy.getShip().s,m_closestWaypoint.s);
        if(isThereLineOfSight)
        {
            int bestAction = manageStraightTravel(a_gameCopy);
            return bestAction;
        }
        else
        {
            return m_rnd.nextInt(Controller.NUM_ACTIONS);
        }
    }

    /**
     * Manages straight travelling.
     * @param a_gameCopy the game copy
     * @return the id of the best action to execute.
     */
    private int manageStraightTravel(Game a_gameCopy)
    {
        int bestAction = Controller.ACTION_NO_FRONT;
        Vector2d dirToWaypoint = m_closestWaypoint.s.copy();
        dirToWaypoint.subtract(a_gameCopy.getShip().s);
        double distance = dirToWaypoint.mag();
        dirToWaypoint.normalise();

        //Check if we are facing the waypoint we are going after.
        Vector2d dir = a_gameCopy.getShip().d;
        boolean notFacingWaypoint = dir.dot(dirToWaypoint) < 0.85;

        //Depending on the time left and the distance to the waypoint, we established the maximum speed.
        //(going full speed could make the ship to overshoot the waypoint... that's the reason of this method!).
        double maxSpeed = 0.4;
        if(distance>100 || a_gameCopy.getStepsLeft() < 50)
            maxSpeed = 0.8;
        else if(distance<30) maxSpeed = 0.25;


        if(notFacingWaypoint || a_gameCopy.getShip().v.mag() > maxSpeed)
        {
            //We should not risk to throttle. Let's rotate in place to face the waypoint better.
            Game forThisAction;
            double bestDot = -2;
            for(int i = Controller.ACTION_NO_FRONT; i <= Controller.ACTION_NO_RIGHT; ++i)
            {
                //Select the action that maximises my dot product with the target (aka. makes the ship face the target better).
                forThisAction = a_gameCopy.getCopy();
                forThisAction.getShip().update(i);
                Vector2d potentialDirection = forThisAction.getShip().d;
                double newDot = potentialDirection.dot(dirToWaypoint);
                if(newDot > bestDot)
                {
                    bestDot = newDot;
                    bestAction = i;
                }
            }
        } else //We can thrust
            return Controller.ACTION_THR_FRONT;

        //There we go!
        return bestAction;
    }


    /**
     * Calculates the closest waypoint to the ship.
     * @param a_gameCopy the game copy.
     */
    private void calculateClosestWaypoint(Game a_gameCopy)
    {
        double minDistance = Double.MAX_VALUE;
        for(Waypoint way: a_gameCopy.getWaypoints())
        {
            if(!way.isCollected())     //Only consider those not collected yet.
            {
                double fx = way.s.x-a_gameCopy.getShip().s.x, fy = way.s.y-a_gameCopy.getShip().s.y;
                double dist = Math.sqrt(fx*fx+fy*fy);
                if( dist < minDistance )
                {
                    //Keep the minimum distance.
                    minDistance = dist;
                    m_closestWaypoint = way;
                }
            }
        }
    }

}
