package framework.core;

import framework.utils.Vector2d;

import java.awt.*;

/**
 * This is a base class for all objects in the game.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public abstract class GameObject {

    /**
     * Position of the object.
     */
    public Vector2d s;

    /**
     * Position of the object in the last step.
     */
    public Vector2d ps;

    /**
     * Velocity of the object.
     */
    public Vector2d v;

    /**
     * Direction vector of the object.
     */
    public Vector2d d;

    /**
     * Reference to the game object.
     */
    Game m_game;

    /**
     * Radius of the object (default: 1).
     */
    public int radius = 1;


    /**
     * Default constructor of the object.
     */
    protected GameObject() {
        s = new Vector2d();
        ps = new Vector2d();
        v = new Vector2d();
        d = new Vector2d();
    }

    /**
     * Constructor for the object, with parameters.
     * @param a_game Reference to the game.
     * @param s position of the object.
     * @param v velocity of the object.
     * @param d orientation of the object.
     */
    protected GameObject(Game a_game, Vector2d s, Vector2d v, Vector2d d) {
        this.m_game = a_game;
        this.s = new Vector2d(s);
        this.ps = new Vector2d(s);
        this.v = new Vector2d(v);
        this.d = new Vector2d(d);
    }

    /**
     * Function to be called every cycle to execute the logic of the object.
     */
    public abstract void update();

    /**
     * Function to be called to paint the object on the screen.
     * @param g Graphics device.
     */
    public abstract void draw(Graphics2D g);

    /**
     * Function to reset the object.
     */
    public abstract void reset();


}