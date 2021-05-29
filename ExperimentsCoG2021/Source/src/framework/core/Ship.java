package framework.core;

import framework.utils.Vector2d;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

/**
 * This class represents the Ship object that the controller moves around the map. Checks for collisions and updates
 * the position, velocity and orientation after applying an action.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class Ship extends GameObject
{
    /**
     * x-coordinates for the ship's shape.
     */
    public final int[] xp = {-2, 0, 2, 0};

    /**
     * y-coordinates for the ship's shape.
     */
    public final int[] yp = {2, -2, 2, 0};

    /**
     * x-coordinates for the ship's thrust shape.
     */
    public final int[] xpThrust = {-2, 0, 2, 0};

    /**
     * y-coordinates for the ship's thrust shape.
     */
    public final int[] ypThrust = {2, 3, 2, 0};

    /**
     * Radians rotated in one action (that implies rotation, obviously).
     */
    public final static double steerStep = Math.PI / 60;

    /**
     *  Friction value of the ship.
     */
    public final static double loss = 0.99;

    /**
     * Radius of the ship.
     */
    public final static int SHIP_RADIUS = 3;

    /**
     * Indicates if the game has started or not.
     */
    private boolean m_started = false;

    /**
     * List of all the actions executed so far(to save replies)
     */
    private ArrayList<Integer> m_actionList;

    /**
     * Next action to be executed.
     */
    private int m_nextMove;
    

    /**
     * Position of the ship after applying a movement BUT before checking for collisions.
     */
    private Vector2d m_potentialPosition;

    /**
     * Speed of the ship after applying a movement BUT before checking for collisions.
     */
    private Vector2d m_potentialSpeed;

    /**
     * Indicates if the current action involves acceleration.
     */
    private boolean m_thrusting;

    /**
     * Indicates if the current action involves rotation.
     */
    private int m_turning;

    /**
     * Indicates if there was a collision in previous step.
     */
    private boolean m_collisionLastStep;

    /**
     * Color of the chassis of the ship.
     */
    private final Color chassisColor = Color.cyan;

    /**
     * Color of the thrust of the ship.
     */
    private final Color thrustColor = Color.green;

    /**
     * Collision sphere of the ship. It represents relative points around the ship that compose the "sphere" (local coordinates).
     */
    private Vector2d m_collSphereRelative[];

    /**
     * Collision sphere of the ship. It represents points around the ship that compose the "sphere" (world coordinates).
     */
    private Vector2d m_collSphere[];

    /**
     * Collision sphere of the ship, located in the world position after applying a move BUT before checking for collisions.
     */
    private Vector2d m_collPotentialSphere[];


    /**
     * Private constructor, only for getCopy()
     */
    private Ship()
    {
        m_actionList = new ArrayList<Integer>();
    }

    /**
     * Ship constructor
     * @param a_game Reference to the game object.
     * @param a_startPos Start position of the ship.
     */
    public Ship(Game a_game, Vector2d a_startPos)
    {
        m_game = a_game;
        s = new Vector2d(a_startPos);
        ps = s.copy();
        v = new Vector2d(0,0);
        d = new Vector2d(0, -1);
        this.radius = SHIP_RADIUS;
        m_actionList = new ArrayList<Integer>();
        m_collisionLastStep = false;
        m_nextMove = Controller.ACTION_NO_FRONT;

        createCollSphere();
    }

    /**
     * Creates the collision sphere for the ship.
     */
    public void createCollSphere()
    {
        int numPoints = 16;
        double angle = 2.0 * Math.PI / numPoints;
        m_collSphereRelative = new Vector2d[numPoints];
        m_collSphere = new Vector2d[numPoints];
        m_collPotentialSphere = new Vector2d[numPoints];

        m_collSphereRelative[0] = d.copy();
        m_collSphereRelative[0].mul(1.5*radius);
        m_collSphere[0] = new Vector2d();
        m_collPotentialSphere[0] = new Vector2d();
        for(int i = 1; i < m_collSphereRelative.length; ++i)
        {
            m_collSphereRelative[i] = m_collSphereRelative[i-1].copy();
            m_collSphereRelative[i].rotate(angle);
            m_collSphere[i] = new Vector2d();
            m_collPotentialSphere[i] = new Vector2d();
        }

        updateCollSphere();
    }

    /**
     * Updates the collision sphere for the next step.
     */
    private void updateCollPotentialSphere()
    {
        for(int i = 0; i < m_collSphereRelative.length; ++i)
        {
            m_collPotentialSphere[i].x = m_collSphereRelative[i].x + m_potentialPosition.x;
            m_collPotentialSphere[i].y = m_collSphereRelative[i].y + m_potentialPosition.y;
        }
    }

    /**
     * Sets the collision sphere to m_collPotentialSphere (to be called after checking for collisions).
     */
    private void updateCollSphere()
    {
        for(int i = 0; i < m_collSphereRelative.length; ++i)
        {
            m_collSphere[i].x = m_collPotentialSphere[i].x;
            m_collSphere[i].y = m_collPotentialSphere[i].y;
        }
    }

    /**
     * Called every cycle to update position and speed, by applying the last move supplied by the controller.
     */
    public void update()
    {
        update(m_nextMove);
    }

    /**
     * Updates position and manages collisions.
     * @param a_actionId Action to execute.
     */
    public void update(int a_actionId)
    {
        if(!m_started)
        {
            if(a_actionId != Controller.ACTION_NO_FRONT)
            {
                m_started = true;
                m_game.go();
            }
            else
                return;
        }

        ps = s.copy();
        m_potentialPosition = s.copy();
        m_potentialSpeed = v.copy();
        m_thrusting = Controller.getThrust(a_actionId);
        m_turning = Controller.getTurning(a_actionId);

        d.rotate(m_turning * steerStep);
        if(m_thrusting)
            m_potentialSpeed.add(d, PTSPConstants.T * 0.05 / 2);
        m_potentialSpeed.mul(loss);
        m_potentialPosition.add(m_potentialSpeed);
        m_collisionLastStep = false;

        //Update the potential position of the collision sphere
        updateCollPotentialSphere();

        //Check for map boundaries:
        checkBoundaries();

        int coll = checkCollisions();
        if(coll != 0)
        {
            //There is collision
            m_collisionLastStep = true;
            if(coll == 1)
                v.x *= (-1);
            else
                v.y *= (-1);

            v.mul(PTSPConstants.COLLISION_SPEED_RED);
        }


        //Check for collisions
        if(!m_collisionLastStep)
        {
            s = m_potentialPosition.copy();
            v = m_potentialSpeed.copy();

            //Update the position of the collision sphere
            updateCollSphere();
        }


        //Add the action to the list of actions.
        m_actionList.add(a_actionId);

        //Check for visited waypoints.
        for(int i=0; i < m_game.getWaypoints().size(); ++i)
        {
            Waypoint way = m_game.getWaypoints().get(i);
            if(!way.collected)
            {
                boolean collected = way.checkCollected(this.s, this.radius);
                if(collected)
                {
                    way.setCollected(true);
                    m_game.addCollected(i);
                }
            }
        }

    }

    /**
     * Checks the boundaries of the map
     */
    private void checkBoundaries()
    {
        if(m_potentialPosition.x > m_game.getMap().getMapChar().length-1)
        {
            m_potentialPosition.x = m_game.getMap().getMapChar().length-1;
        }
        else if(m_potentialPosition.x < 0)
        {
            m_potentialPosition.x = 0;
        }
        else if(m_potentialPosition.y > m_game.getMap().getMapChar()[0].length -1)
        {
            m_potentialPosition.y = m_game.getMap().getMapChar()[0].length -1;
        }
        else if(m_potentialPosition.y < 0)
        {
            m_potentialPosition.y = 0;
        }
    }

    /**
     *  Collision estimation, checking all "angle points" of the ship.
     *  @return type of collisions (0: no collision, 1: collision up/down, 2: collision left/right)
     */
    private int checkCollisions()
    {

        for(int i = 0; i < m_collSphere.length; ++i)
        {
            Vector2d v = m_collPotentialSphere[i];
            int collision = checkCollInPos(v);
            if(collision != 0)
            {
                Vector2d vToColl = v.subtract(m_potentialPosition);
                vToColl.normalise();
                Vector2d velocity = m_potentialSpeed.copy();
                velocity.normalise();

                //This is to slide when in contact with walls instead of being stuck.
                double dotProduct = velocity.dot(vToColl);
                if(dotProduct > 0.5)
                    return collision;
            }
        }

        return 0;
    }

    /**
     * Checks if there is a collision in the given position, considering the ship's bounding sphere
     * @param a_position Central position of the ship (or the point to check).
     * @return true if there is collision.
     */
    public boolean checkCollisionInPosition(Vector2d a_position)
    {
        Vector2d position = new Vector2d();
        for(int i = 0; i < m_collSphere.length; ++i)
        {
            position.x = m_collSphereRelative[i].x + a_position.x;
            position.y = m_collSphereRelative[i].y + a_position.y;
            int collision = checkCollInPos(position);
            if(collision != 0)
                return true;
        }
        return false;
    }

    /**
     * Checks if there is a collision in one point in the world.
     * @param a_collPoint Point in the world to check.
     * @return the type of collision (0: no collision, 1: collision up/down, 2: collision left/right)
     */
    private int checkCollInPos(Vector2d a_collPoint)
    {
        int xRound = (int)Math.round(a_collPoint.x);
        int yRound = (int)Math.round(a_collPoint.y);

        if(m_game.getMap().isOutsideBounds(xRound,yRound))
            return 1;

        if(m_game.getMap().isObstacle(xRound, yRound))
        {
            if(m_game.getMap().isCollisionUpDown(xRound,yRound))
            {
                return 1;
            }
            else
            {
                return 2;
            }
        }
        return 0;
    }


    /**
     * Draws the ship on the screen.
     * @param g Graphics object.
     */
    public void draw(Graphics2D g)
    {
        AffineTransform at = g.getTransform();
        g.translate(s.x, s.y);

        double rot = Math.atan2(d.y, d.x) + Controller.HALF_PI;
        g.rotate(rot);
        g.scale(radius, radius);
        g.setColor(chassisColor);
        g.fillPolygon(xp, yp, xp.length);
        if (m_thrusting) {
            g.setColor(thrustColor);
            g.fillPolygon(xpThrust, ypThrust, xpThrust.length);
        }

        g.setTransform(at);
        //printDebug(g);
    }

    /**
     * Prints the collision sphere and the center of the ship.
     * @param g Graphics object.
     */
    private void printDebug(Graphics2D g)
    {
        //Collision sphere
        g.setColor(Color.yellow);
        for(int i = 0; i < m_collSphere.length; ++i)
        {
            Vector2d v = m_collSphere[i];
            g.drawOval((int) Math.round(v.x),(int) Math.round(v.y), 2, 2);
        }

        //CENTER OF THE SHIP  (real position in map).
        g.setColor(Color.red);
        g.drawOval((int) (s.x),(int) (s.y), 2, 2);
    }

    /**
     * Resets the ship.
     */
    public void reset() {
        //Not in use, nothing to do here.
    }

    /************* Getters and Setters **********************/

    /**
     * Returns all the actions executed so far.
     * @return all the actions executed so far.
     */
    public ArrayList getActionList() {return m_actionList;}

    /**
     * Returns if there was a collision in the last step.
     * @return if there was a collision in the last step.
     */
    public boolean getCollLastStep() {return m_collisionLastStep;}

    /**
     * Indicates if the game has started or not
     * @return if the game has started or not
     */
    public boolean hasStarted() {return m_started;}

    /**
     * Gets a copy the position of the ship before checking for collisions.
     * @return the potential position of the ship.
     */
    public Vector2d getPotentialPosition() {return m_potentialPosition.copy();}

    /**
     * Gets a copy of the speed of the ship before checking for collisions.
     * @return the potential velocity of the ship.
     */
    public Vector2d getPotentialSpeed() {return m_potentialSpeed.copy();}

    /**
     * Indicates if the last action involved acceleration.
     * @return if the last action involved acceleration.
     */
    public boolean isThrusting() {return m_thrusting;}

    /**
     * Indicates if the last action involved rotation.
     * @return if the last action involved rotation.
     */
    public int turning() {return m_turning;}


    /**
     * Sets if the game has started.
     * @param a_st if the game has started.
     */
    public void setStarted(boolean a_st) { m_started = a_st;}

    /**
     * Sets if there was a collision in this step.
     * @param a_collLastStep if there was a collision in this step or not.
     */
    public void setCollisionLastStep(boolean a_collLastStep) {m_collisionLastStep = a_collLastStep;}

    /**
     * Sets the next move to apply.
     * @param a_nextMove the next move to apply.
     */
    public void setNextMove(int a_nextMove) {m_nextMove = a_nextMove;}

    /**
     * Adds a new action to the list of actions executed so far.
     * @param a_action new action to add.
     */
    public void addAction(int a_action) {m_actionList.add(a_action);}

    /**
     * Gets a copy of the game.
     * @param a_game A copy of the game state
     * @return The copy of the ship.
     */
    public Ship getCopy(Game a_game)
    {
        Ship copied = new Ship();

        for(int i = 0; i < m_actionList.size(); ++i)
        {
            copied.addAction(m_actionList.get(i));
        }

        copied.s = this.s.copy();
        copied.v = this.v.copy();
        copied.ps = this.ps.copy();
        copied.d = this.d.copy();
        copied.m_game = a_game;
        copied.radius = this.radius;

        copied.setStarted(m_started);
        copied.setCollisionLastStep(m_collisionLastStep);

        //Create and copy all collision spheres
        copied.createCollSphere();
        copied.copyCollSph(m_collSphere);

        return copied;
    }

    /**
     * Copies an array of positions to the collision sphere of the ship
     * @param a_collRel the array of positions.
     */
    public void copyCollSph(Vector2d a_collRel[])
    {
        for(int i = 0; i < a_collRel.length; ++i)
        {
            m_collSphere[i] = a_collRel[i].copy();
        }
    }



}
