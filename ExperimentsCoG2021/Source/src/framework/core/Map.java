package framework.core;

import framework.utils.File2String;
import framework.utils.Vector2d;
import javax.swing.*;
import java.util.LinkedList;

/**
 * This class represents the map where the game is played. An interesting method of this class is LineOfSight, that can be used
 * to check the line of sight between two points in the map.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class Map extends JComponent
{

    /**
     * Symbol for an edge of the map.
     */
    public static final char EDGE = 'E';

    /**
     * Symbol for the starting position of the ship in the map.
     */
    public static final char START = 'S';

    /**
     * Symbol for a waypoint in the map.
     */
    public static final char WAYPOINT = 'C';

    /**
     * Symbol for an empty space in the map.
     */
    public static final char NIL = '.';

    /**
     * Array with all the elements of the map.
     */
    private char m_mapChar[][];

    /**
     * Height, in pixels, of the map.
     */
    private int m_height;

    /**
     * Width, in pixels, of the map.
     */
    private int m_width;

    /**
     * Starting point of the ship in the map.
     */
    private Vector2d m_startingPoint;

    /**
     * List of the positions of the waypoints.
     */
    private LinkedList<Vector2d> m_waypointPos;

    /**
     * Reference to the game.
     */
    private Game m_game;

    /**
     * Filename where this map is read from.
     */
    private String m_filename;

    /**
     * Private constructor, only used by getCopy()
     */
    private Map()
    {
        m_waypointPos = new LinkedList<Vector2d>();
    }

    /**
     * Map constructor
     * @param a_game reference to the game.
     * @param a_filename filename to read the map from.
     */
    public Map(Game a_game, String a_filename)
    {
        this.m_game = a_game;
        m_filename = a_filename;
        m_startingPoint = new Vector2d();
        m_waypointPos = new LinkedList<Vector2d>();
        readMap();
    }


    /**
     * Map constructor, from data structures.
     * @param a_game reference to the game.
     * @param map map contents.
     * @param startingPoint Starting point of the ship.
     * @param wayPoints Position of the waypoints in the map.
     */
    public Map(Game a_game, char[][] map, Vector2d startingPoint, LinkedList<Vector2d> wayPoints)
    {
        this.m_game = a_game;
        m_startingPoint = startingPoint.copy();
        m_waypointPos = new LinkedList<Vector2d>();

        m_mapChar = new char[map.length][map[0].length];
        for(int i = 0; i < map.length; ++i)
        {
            for(int j = 0; j < map[0].length; ++j)
            {
                m_mapChar[i][j] = map[i][j];
            }
        }

        m_width = map.length;
        m_height = map[0].length;


        for(int i = 0; i < wayPoints.size(); ++i)
        {
            Waypoint newWaypoint = new Waypoint(m_game, wayPoints.get(i));
            m_mapChar[(int) newWaypoint.s.x][(int)newWaypoint.s.y] = Map.WAYPOINT;
            m_waypointPos.add(wayPoints.get(i));
        }

        m_mapChar[(int) m_startingPoint.x][(int)m_startingPoint.y] = Map.START;

    }

    /**
     * Reads the map.
     */
    private void readMap()
    {
        String[][] fileData = File2String.getArray(m_filename);

        int x = 0, xInMap = 0;
        String[] line;
        while(x < fileData.length)
        {
            line = fileData[x]; //Get following line.

            String first = line[0];
            if(first.equalsIgnoreCase("type"))
            {
                //Ignore
            }else if(first.equalsIgnoreCase("height"))
            {
                String h = line[1];
                m_height = Integer.parseInt(h);
            }
            else if(first.equalsIgnoreCase("width"))
            {
                String w = line[1];
                m_width = Integer.parseInt(w);
            }
            else if(first.equalsIgnoreCase("map"))
            {
                //Ignore ... but time to create the map
                m_mapChar = new char[m_width][m_height];
                //System.out.println("Map dimensions: " + m_width + "x" + m_height);
            }
            else
            {
                //MAP INFORMATION
                String lineStr = line[0];
                int yInMap = 0;
                int yInFile = 0;
                while(yInMap < lineStr.length())
                {
                    char data = lineStr.charAt(yInFile);
                    //System.out.println(xInMap + "," + yInMap + ": " + data);
                    m_mapChar[yInMap][xInMap] = data;

                    processData(yInMap, xInMap, data);

                    ++yInMap;
                    ++yInFile;
                }
                ++xInMap;
            }

            ++x;
        }
    }

    /**
     * Process a given character in a position
     * @param x x coordinate
     * @param y y coordinate
     * @param data data read on the map.
     */
    private void processData(int x, int y, char data)
    {
        if(data == START)
        {
            m_startingPoint.x=x;
            m_startingPoint.y=y;
        }
        else if(data == WAYPOINT)
        {
            Vector2d newCol = new Vector2d(x,y);
            Waypoint way = new Waypoint(m_game, newCol);
            if(m_game == null)
                m_game = new Game();
            m_game.addWaypoint(way);
            m_waypointPos.add(new Vector2d(x,y));
        }
    }

    /**
     * Checks if the given point is outside the bounds of the map.
     * @param a_x x ccoordinate
     * @param a_y y coordinate.
     * @return true if the position is outside the map.
     */
    public boolean isOutsideBounds(int a_x, int a_y)
    {
        return (a_x < 0 || a_x >= m_mapChar.length || a_y < 0 || a_y >= m_mapChar[a_x].length);
    }

    /**
     * Checks if there is an obstacle in the given position.
     * @param a_x x ccoordinate
     * @param a_y y coordinate.
     * @return true if there is an obstacle in the givven position.
     */
    public boolean isObstacle(int a_x, int a_y)
    {
        char inMap = m_mapChar[a_x][a_y];
        return isObstacle(inMap);
    }

    /**
     * Checks if the given character represents an obstacle
     * @param data character
     * @return true if it represents an obstacle.
     */
    private boolean isObstacle(char data)
    {
        if(data == '@' || data == 'T' || data == '\u0000' || data == Map.EDGE)
            return true;
        return false;
    }

    /**
     * Checks if the collision at a_x, a_y belongs to a vertical or an horizontal wall.
     * @param a_x x coordinate of the collision point
     * @param a_y y coordinate of the collision point
     * @return true if the collision is against a vertical wall
     */
    public boolean isCollisionUpDown(int a_x, int a_y)
    {
        int consUpDown = 1, consRightLeft = 1; //we suppose there is collision in (a_x, a_y)
        if(a_y+1 < m_mapChar[a_x].length)
        {
            consUpDown += isObstacle(m_mapChar[a_x][a_y+1])? 1:0;
        }
        if(a_y-1 >= 0)
        {
            consUpDown += isObstacle(m_mapChar[a_x][a_y-1])? 1:0;
        }

        if(a_x+1 < m_mapChar.length)
        {
            consRightLeft += isObstacle(m_mapChar[a_x+1][a_y])? 1:0;
        }
        if(a_x-1 >= 0)
        {
            consRightLeft += isObstacle(m_mapChar[a_x-1][a_y])? 1:0;
        }

        return consUpDown>consRightLeft;
    }

    /**
     * Checks for the distance to the closest obstacle given a direction, from the origin, up to a maximum distance.
     * @param a_origin  Position to check from.
     * @param a_direction Direction to check from a_origin
     * @param a_maxDistance Max distance to check. There could be obstacles further the specified distance. Longer distances
     *                      mean longer execution time for this method in the worst case (where there are no obstacles close).
     * @return The distance to the closest obstacle, in the given direction. If there is no obstacle at a minor distance that the
     * value specified in a_maxDistance, -1 is returned.
     */
    public double distanceToCollision(Vector2d a_origin, Vector2d a_direction, int a_maxDistance)
    {
        //Direction must be normalized.
        a_direction.normalise();
        Vector2d finalPosition = a_origin.copy();
        finalPosition.add(a_direction, a_maxDistance);

        return checkObsFreeDistance((int) a_origin.x, (int) a_origin.y, (int) finalPosition.x,(int) finalPosition.y, Ship.SHIP_RADIUS);
    }


    /**
     * Checks if there is line of sight between two positions (takes radius of the ship into account).
     * @param a_origin Origin position.
     * @param a_destination Destination position.
     * @return true if there is no obstacle between origin and destination.
     */
    public boolean LineOfSight(Vector2d a_origin, Vector2d a_destination)
    {
        return checkObsFree((int) a_origin.x, (int) a_origin.y, (int) a_destination.x,(int) a_destination.y);
    }

    /**
     * Checks if, between two positions, there is no obstacle, taking into account the radius of the ship.
     * @param a_orgX start x coordinate
     * @param a_orgY start ycoordinate
     * @param a_destX end x coordinate
     * @param a_destY end y coordinate
     * @return true if there is no collision from (a_orgX,a_orgY) to (a_destX, a_destY).
     */
    public boolean checkObsFree(int a_orgX, int a_orgY, int a_destX, int a_destY)
    {
        return (checkObsFreeDistance(a_orgX, a_orgY, a_destX, a_destY, Ship.SHIP_RADIUS) == -1);
    }


    /**
     * Checks if, betweeen two positions, there is no obstacle, taking into account the radius of the ship.
     * Returns the distance to the fist obstacle found, or -1 if no obstacle.
     * @param a_orgX start x coordinate
     * @param a_orgY start ycoordinate
     * @param a_destX end x coordinate
     * @param a_destY end y coordinate
     * @param a_increment Step in pixels to advance from origin to destination (smaller, the more accurate, but slower)
     * @return the distance to the fist obstacle found, or -1 if no obstacle.
     */
    public double checkObsFreeDistance(int a_orgX, int a_orgY, int a_destX, int a_destY, int a_increment)
    {
        double increment = a_increment;
        Vector2d dir = new Vector2d(a_destX - a_orgX, a_destY - a_orgY);
        double distance = dir.mag();
        dir.normalise();
        dir.mul(increment);
        double acum = increment;

        Vector2d pos = new Vector2d(a_orgX, a_orgY);
        while(acum < distance)
        {
            pos.add(dir);

            if( m_game.getShip().checkCollisionInPosition(pos))
                return acum;
            acum += increment;
        }

        return -1;
    }


    /*** Getters and setters ***/

    public LinkedList<Vector2d> getWaypointPositions()
    {
        return m_waypointPos;
    }

    /**
     * Gets the array representing the map
     * @return the array representing the map
     */
    public char[][] getMapChar() {return m_mapChar; }

    /**
     * Gets the starting point in the map
     * @return the starting point.
     */
    public Vector2d getStartingPoint() {return m_startingPoint.copy();}

    /**
     * Gets the height of the map.
     * @return the height of the map.
     */
    public int getMapHeight() {return m_height;}

    /**
     * Gets the width of the map.
     * @return the width of the map.
     */
    public int getMapWidth() {return m_width;}

    /**
     * Gets the filename this map was loaded from.
     * @return the filename this map was loaded from.
     */
    public String getFilename() {return m_filename;}


    /**
     * Sets the map character array.
     * @param a_mapChar the map character array.
     */
    public void setMapChar(char[][] a_mapChar){m_mapChar = a_mapChar;}

    /**
     * Sets the height of the map.
     * @param a_h the height of the map.
     */
    public void setHeight(int a_h) {m_height = a_h;}

    /**
     * Sets the width of the map.
     * @param a_w the width of the map.
     */
    public void setWidth(int a_w) {m_width = a_w;}

    /**
     * Sets the starting point of the ship in the map.
     * @param a_sp the starting point of the ship in the map.
     */
    public void setStartingPoint(Vector2d a_sp) {m_startingPoint = a_sp;}

    /**
     * Sets the game instance
     * @param a_game the game instance
     */
    public void setGame(Game a_game) {m_game = a_game;}

    /**
     * Sets the filename to read the map from.
     * @param a_filename the filename where the map is.
     */
    public void setFilename(String a_filename) {m_filename = a_filename;}

    /**
     * Gets a copy of the map.
     * @param a_game the game isntance.
     * @return The map copy.
     */
    public Map getCopy(Game a_game)
    {
        Map copied = new Map();

        //Copy the map
        char[][] mapChar = new char[m_mapChar.length][m_mapChar[0].length];
        for(int i = 0; i < mapChar.length; ++i)
            for(int j = 0; j < mapChar[0].length; ++j)
                mapChar[i][j] = m_mapChar[i][j];
        copied.setMapChar(mapChar);

        copied.setHeight(m_height);
        copied.setWidth(m_width);
        copied.setStartingPoint(m_startingPoint.copy());
        copied.setGame(a_game);
        copied.setFilename(m_filename); //Not copied, no need.

        return copied;
    }

}

