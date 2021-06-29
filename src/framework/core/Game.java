package framework.core;

import controllers.autoSubgoalMCTS.AbstractController;
import framework.utils.Vector2d;

import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.*;

/**
 * Game class. This class holds the core functionality of the game, including game objects and game update cycle.
 * An instance of Game is passed every cycle to the controller, in order to provide enough information about the environment
 * (including map, waypoints and ship) so the controller can take the appropriate action. That instance is a copy of the
 * current game, and it can be modified and used to run simulations.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class Game
{
    public static double randomActionProbability = 0;

    /**
     * Map of the game.
     */
    public static Map[] m_maps;

    /**
     * Number of current map.
     */
    private static int m_currentMap;

    /**
     * Dimensions of the map.
     */
    private Dimension m_size;

    /**
     * Ship of the game.
     */
    private Ship m_ship;

    /**
     * Steps left for reaching a new waypoint
     */
    private int m_stepsLeft;

    /**
     * Total time spent travelling through the map.
     */
    private int m_totalTime;

    /**
     * Indicates if the game has started (ship has made a move).
     */
    private boolean m_started;

    /**
     * Waypoints left to the end
     */
    private int m_waypointsLeft;

    /**
     * Indicates if the game is ended
     */
    private boolean m_gameEnded;

    /**
     * List of waypoints in the map.
     */
    private ArrayList<Waypoint> m_waypoints;

    /**
     * Order of waypoints visited so far.
     */
    private ArrayList<Integer> m_visitOrder;

    /**
     * Default ccnstructor. Only used for getCopy().
     */
    public Game()
    {
        m_waypoints = new ArrayList<>();
        m_visitOrder = new ArrayList<Integer>();
    }

    /**
     * Public game constructor
     * @param a_mapFilename Name of the file where the game will be played.
     */
    public Game(String a_mapFilename)
    {
        //This is the number of steps allowed until reaching the next waypoint.
        m_stepsLeft = PTSPConstants.STEPS_PER_WAYPOINT;

        //Total time
        m_totalTime = 0;

        //It'll be started when the ship makes a move.
        m_started = m_gameEnded = false;

        //List of waypoints of the map.
        m_waypoints = new ArrayList<>();

        //Order of visits.
        m_visitOrder = new ArrayList<Integer>();

       //Create and read the map.
        m_currentMap = 0;
        m_maps = new Map[1];
        m_maps[m_currentMap] = new Map(this, a_mapFilename);

        //Initialize some variables
        m_size = new Dimension(getMap().getMapChar().length, getMap().getMapChar()[0].length);

        //Create the ship of the game and add it to the game objects
        m_ship = new Ship(this, getMap().getStartingPoint());

        m_waypointsLeft = m_waypoints.size();
    }

    /**
     * Builds a map from data structures.
     * @param map Map contents (just obstacles and free spaces).
     * @param startingPoint Starting point of the ship.
     * @param wayPoints List of waypoint positions.
     */
    public Game(char[][] map, Vector2d startingPoint, LinkedList<Vector2d> wayPoints)
    {
        //This is the number of steps allowed until reaching the next waypoint.
        m_stepsLeft = PTSPConstants.STEPS_PER_WAYPOINT;

        //Total time
        m_totalTime = 0;

        //It'll be started when the ship makes a move.
        m_started = m_gameEnded = false;

        //Order of visits.
        m_visitOrder = new ArrayList<Integer>();

        //List of waypoints of the map.
        m_waypoints = new ArrayList<>();

        //Copy the waypoints.
        for(int i = 0; i < wayPoints.size(); ++i)
        {
            Waypoint newWaypoint = new Waypoint(this, wayPoints.get(i));
            addWaypoint(newWaypoint);
        }

        //Create and read the map.
        m_currentMap = 0;
        m_maps = new Map[1];
        m_maps[m_currentMap] = new Map(this, map, startingPoint, wayPoints);

        //Initialize some variables
        m_size = new Dimension(getMap().getMapChar().length, getMap().getMapChar()[0].length);

        //Create the ship of the game and add it to the game objects
        m_ship = new Ship(this, getMap().getStartingPoint());
    }

    /**
     * Public game constructor
     * @param a_mapFilename Name of the file where the game will be played.
     */
    public Game(String[] a_mapFilename)
    {
        //This is the number of steps allowed until reaching the next waypoint.
        m_stepsLeft = PTSPConstants.STEPS_PER_WAYPOINT;

        //Total time
        m_totalTime = 0;

        //It'll be started when the ship makes a move.
        m_started = m_gameEnded = false;

        //Order of visits.
        m_visitOrder = new ArrayList<Integer>();

        //List of waypoints of the map.
        m_waypoints = new ArrayList<>();

        //Create and read the map.
        if(m_maps == null)
        {
            //FIRST TIME:
            m_currentMap = 0;
            m_maps = new Map[a_mapFilename.length];
            for(int i = 0; i < m_maps.length; ++i)
            {
                m_maps[i] = new Map(this, a_mapFilename[i]);
            }
        }else if(m_currentMap < m_maps.length)
        {
            LinkedList<Vector2d> positions = m_maps[m_currentMap].getWaypointPositions();
            for(Vector2d pos : positions)
            {
                addWaypoint(new Waypoint(this,pos));
            }
        }

        //Initialize some variables
        m_size = new Dimension(getMap().getMapChar().length, getMap().getMapChar()[0].length);

        //Create the ship of the game and add it to the game objects
        m_ship = new Ship(this, getMap().getStartingPoint());
    }

    public void tickRandom(int action)
    {
        if(AbstractController.rng.nextDouble() < randomActionProbability)
        {
            tick(AbstractController.rng.nextInt(Controller.NUM_ACTIONS));
        }
        else
        {
            tick(action);
        }
    }

    /**
     * Updates the game logic: all entities in the game and game state.
     * @param a_shipMove Move to execute.
     */
    public void tick(int a_shipMove)
    {
        //Update all entities of the game, including player's ship.
        m_ship.update(a_shipMove);

        //One step left to the end.
        if(m_started)
        {
            m_stepsLeft--;
            m_totalTime++;
        }

        //Check for end of the game.
        if(m_waypointsLeft == 0 || m_stepsLeft <= 0)
            m_gameEnded = true;

    }


    /**
     * Updates the game when a waypoint is collected.
     */
    public void waypointCollected()
    {
        m_stepsLeft = PTSPConstants.STEPS_PER_WAYPOINT;
        m_waypointsLeft--;
    }

    /**
     * Adds a waypoint to the list of collected
     * @param a_index Index in the array list of waypoints of the waypoint collected.
     */
    public void addCollected(int a_index)
    {
        m_visitOrder.add(a_index);
    }

    /**
     * Saves a route to file.
     */
    public void saveRoute()
    {
        String routeFile = getSaveFilename();
        try {
            PrintWriter out = new PrintWriter(new FileWriter(routeFile));
            out.println(m_ship.getActionList().size());
            for (int i = 0; i < m_ship.getActionList().size(); i++) {
                out.println(m_ship.getActionList().get(i));
            }
            out.close();
            System.out.println("Route saved to: " + routeFile);
        } catch (Exception e) {
            System.out.println("Exception saving route: " + routeFile);
        }

    }


    /**
     * Saves a route to file.
     * @param filename File to write the route.
     */
    public void saveRoute(String filename)
    {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(filename));
            out.println(m_ship.getActionList().size());
            for (int i = 0; i < m_ship.getActionList().size(); i++) {
                out.println(m_ship.getActionList().get(i));
            }
            out.close();
            System.out.println("Route saved to: " + filename);
        } catch (Exception e) {
            System.out.println("Exception saving route: " + filename);
        }

    }

    /**
     * Gets a string with the current date for the save filename.
     * @return The name of the file to write the actions to.
     */
    private String getSaveFilename()
    {
        Date now = new Date();
        String routeFile = "route_" + DateFormat.getDateInstance().format(now) + "_" + DateFormat.getTimeInstance().format(now) + ".txt";
        return routeFile.replace(":",".");
    }

    /**
     * Prints the final results to the output console.
     */
    public void printResults()
    {
        if(getWaypointsLeft() == 0)
        {
            System.out.println("Final score: " + getWaypointsVisited() +
                    " waypoints in " + getTotalTime() + " steps.");
        }else if(getStepsLeft() <= 0)
        {
            System.out.println("Time out. Final score: " + getWaypointsVisited() +
                    " waypoints in " + getTotalTime() + " steps.");
        }
    }
    
    /**
     * Returns the number of waypoitns visited in this play.
     * @return the number of waypoitns visited in this play
     */
    public int getWaypointsVisited()
    {
        return (m_waypoints.size() - getWaypointsLeft());
    }

    /**
     * Starts the game (only used in the KeyController execution.)
     */
    public void go(){m_started = true;}

    /**
     * Advances to the next map in the array of loaded maps.
     * @return true if there is a map yet to be played. False if the last map was the last of the maps created.
     * */
    public boolean advanceMap()
    {
        m_currentMap++;

        if(m_currentMap < m_maps.length)
            return true;
        return false;
    }

    /**
     * Adds a new waypoint to the list of waypoints.
     * @param a_way waypoint to add.
     */
    public void addWaypoint(Waypoint a_way)
    {
        m_waypoints.add(a_way);
    }

    /***** GETTERS AND SETTERS ****/

    /**
     * Gets the map of the game.
     * @return map of the game.
     */
    public Map getMap()
    {
        if(m_currentMap < m_maps.length)
            return m_maps[m_currentMap];
        else return null;
    }

    /**
     * Gets the dimension of the map where the game is being played.
     * @return dimensions of the map.
     */
    public Dimension getMapSize() {return m_size;}

    /**
     * Gets the ship object of the game.
     * @return the ship object.
     */
    public Ship getShip() {return m_ship;}

    /**
     * Gets all the waypoints of the map.
     * @return all the waypoints of the map.
     */
    public ArrayList<Waypoint> getWaypoints() {return m_waypoints;}

    /**
     * Returns the number of waypoints yet to be collected.
     * @return the number of waypoints yet to be collected.
     */
    public int getWaypointsLeft() {return m_waypointsLeft;}

    /**
     * Returns the visit order of the game so far (indexes of the waypoints in m_waypoints)
     */
    public ArrayList<Integer> getVisitOrder() {return m_visitOrder;}
    
    
    /**
     * Returns the number of steps before the time runs out, until the next waypoints is visited.
     * @return the number of steps before the time runs out, until the next waypoints is visited.
     */
    public int getStepsLeft() {return m_stepsLeft;}

    /**
     * Gets the number of steps spent since the begining of the execution.
     * @return the number of steps spent since the begining of the execution.
     */
    public int getTotalTime() {return m_totalTime;}

    /**
     * Indicates if the game has started.
     * @return whether the game has started or not.
     */
    public boolean hasStarted() {return m_started;}

    /**
     * Indicates if the game has finished.
     * @return true if the game is over.
     */
    public boolean isEnded() {return m_gameEnded;}

    /**
     * Sets the size of the game map.
     * @param a_size the new size.
     */
    public void setSize(Dimension a_size) {m_size = a_size;}

    /**
     * Sets the ship instance of the game.
     * @param a_ship the ship object.
     */
    public void setShip(Ship a_ship) {m_ship = a_ship;}

    /**
     * Sets the number of steps left to reach the next waypoint.
     * @param a_stepsLeft the number of steps left.
     */
    public void setStepsLeft(int a_stepsLeft) {m_stepsLeft = a_stepsLeft;}

    /**
     * Sets the time of the execution.
     * @param a_totalTime number of steps since the beginning of the execution.
     */
    public void setTotalTime(int a_totalTime) {m_totalTime = a_totalTime;}

    /**
     * Sets the number of waypoints left to end the game.
     * @param a_waypointsLeft  number of waypoints.
     */
    public void setWaypointsLeft(int a_waypointsLeft) {m_waypointsLeft = a_waypointsLeft;}

    /**
     * Sets if the game has started.
     * @param a_started true if the game has started.
     */
    public void setStarted(boolean a_started) {m_started = a_started;}


    /**
     * Gets a copy of the game state.
     * @return a copy of the game.
     */
    public Game getCopy()
    {
        Game copied = new Game();

        copied.setSize(new Dimension(m_size.width, m_size.height));
        copied.setShip(m_ship.getCopy(copied));
        copied.setStepsLeft(m_stepsLeft);
        copied.setTotalTime(m_totalTime);
        copied.setStarted(m_started);
        copied.setWaypointsLeft(m_waypointsLeft);

        //Copy waypoints
        for(Waypoint way : m_waypoints)
        {
            copied.addWaypoint(way.getCopy(copied));
        }
        
        //Copy visit order
        for(Integer i : m_visitOrder)
        {
            copied.addCollected(i);
        }
        return copied;
    }

    public void randomizeWaypoints(int numWaypoints, Random rng)
    {
        m_waypoints.clear();
        for(int i =0; i < numWaypoints; i++)
        {
            int xPos = rng.nextInt(getMapSize().width);
            int yPos = rng.nextInt(getMapSize().height);
            while(isCircleOverlapping(xPos, yPos, Waypoint.RADIUS))
            {
                xPos = rng.nextInt(getMapSize().width);
                yPos = rng.nextInt(getMapSize().height);
            }
            addWaypoint(new Waypoint(this, new Vector2d(xPos, yPos)));
        }
        m_waypointsLeft = numWaypoints;
    }

    private boolean isCircleOverlapping(int xPos, int yPos, int radius)
    {
        for(int dy = -radius / 2; dy < radius / 2; dy++)
        {
            for(int dx = -radius / 2; dx < radius / 2; dx++)
            {
                int x = xPos + dx;
                int y = yPos + dy;
                if(getMap().isOutsideBounds(x, y))
                    continue;

                if(getMap().getMapChar()[x][y] != Map.NIL && getMap().getMapChar()[x][y] != Map.WAYPOINT)
                {
                    return true;
                }
            }
        }

        return false;
    }
}
