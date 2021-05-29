package framework.core;

import framework.utils.Vector2d;
import java.awt.*;

/**
 * This class represents the Waypoint object, that must be collected by the ship during the game.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class Waypoint extends GameObject
{
    /**
     * Indicates if this waypoint has been collected in the game.
     */
    protected boolean collected;


    /**
     * Radius of the waypoint.
     */
    public static int RADIUS = 4;

    /**
     * Private constructor, used by getCopy();
     */
    private Waypoint()
    {
    }

    /**
     * Constructor of the Waypoint.
     * @param game Reference to the game.
     * @param s Position of the waypoint in the map
     */
    public Waypoint(Game game, Vector2d s)
    {
        m_game = game;
        this.s = s;
        this.collected = false;
        this.radius = RADIUS;
    }

    /**
     * Function to be called every cycle.
     */
    public void update() {
        //Nothing to do here.
    }

    /**
     * Resets the waypoint.
     */
    public void reset() {
        //Not in use, nothing to do here.
    }

    /**
     *  Draws the waypoint.
     *  @param g Graphics object.
     */
    public void draw(Graphics2D g)
    {
        if(!collected)
            g.setColor(Color.red);
        else
            g.setColor(Color.blue);

        int drawRadius = Ship.SHIP_RADIUS * radius;
        g.fillOval((int) (s.x - drawRadius*0.5),(int) (s.y - drawRadius*0.5),drawRadius,drawRadius);

        g.setColor(Color.yellow);
        g.fillOval((int) (s.x - radius),(int) (s.y - radius),radius,radius);
    }

    //

    /**
     * Check if this waypoint is collected, given the position of the ship.
     * @param a_pos Position of the ship, or to be checked.
     * @param a_radius Radius of the ship (or distance from the point provided).
     * @return true if the waypoint is collected.
     */
    public boolean checkCollected(Vector2d a_pos, int a_radius)
    {
        double xd = s.x - a_pos.x;
        double yd = s.y - a_pos.y;
        double d = Math.sqrt(xd*xd+yd*yd);

        return d<(a_radius+this.radius);
    }

    /**
     * Sets if the waypoint is collected.
     * @param coll if the waypoint is collected.
     */
    public void setCollected(boolean coll)
    {
        if(!collected)
        {
            collected = coll;
            m_game.waypointCollected();
        }
    }

    /**
     * Indicates if this waypoint is already visted or not.
     * @return if this waypoint has been already collected.
     */
    public boolean isCollected() {return this.collected;}


    /**
     *  Gets a copy of the waypoint.
     *
     * @param a_game Reference to the game object.
     * @return A copy of the waypoint.
     */
    public Waypoint getCopy(Game a_game)
    {
        Waypoint copied = new Waypoint();

        copied.s = this.s.copy();
        copied.v = this.v.copy();
        copied.ps = this.ps.copy();
        copied.d = this.d.copy();
        copied.m_game = a_game;
        copied.radius = this.radius;
        copied.collected = this.collected;

        return copied;
    }

    /**
     * Check if two waypoints are the same
     * @param a_other the other waypoint to check with this.
     * @return true if both waypoints are the same.
     */
    @Override
    public boolean equals(Object a_other)
    {
        Waypoint waypoint = (Waypoint) a_other;
        if(this.s.x == waypoint.s.x && this.s.y == waypoint.s.y)
            return true;
        return false;
    }

    /**
     * Hash code for this waypoint.
     * @return The hash code.
     */
    @Override
    public int hashCode()
    {
        return 100000*(100+(int)this.s.y) + (10000+(int)this.s.x);
    }

}
