package framework.core;

/**
 * This class contains some important constants for the game.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public interface PTSPConstants
{
    /**
     * Delay between time steps, used for replays and human plays.
     * It is set to 16ms, what implies near 62fps (1000/16) = 62.5
     */
    final int DELAY = 16;

    /**
     * Time constant
     */
    final double T = 1.0;

    /**
     * This is the number of steps allowed until reaching the next waypoint.
     */
    final int STEPS_PER_WAYPOINT = 1000;

    /**
     * The velocity of the ship will be multiplied by this amount when colliding with a wall.
     */
    final double COLLISION_SPEED_RED = 0.25;

    /**
     * Time for the controller to be initialized.
     */
    final int INIT_TIME_MS = 10000;

    /**
     * Time for the controller to provide an action every step.
     */
    final int ACTION_TIME_MS = 40;

    /**
     * Interval wait. Used to check for controller replies in some execution modes.
     */
    public static final int INTERVAL_WAIT=1;
}
