package framework.core;

import framework.utils.Vector2d;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

/**
 * This class paints the game on the screen. It is used from the Runner class, who knows if graphics are enabled or not
 * for the execution.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 20/12/11
 */
public class PTSPView extends JComponent
{

    /**
     * Reference to the game to be painted.
     */
    private Game m_game;

    /**
     * Reference to the ship of the game.
     */
    private Ship m_ship;

    /**
     * reference to the map instance where the game is being played.
     */
    private Map m_map;

    /**
     * Dimensions of the map and, hence, of the window.
     */
    private Dimension m_size;

    /**
     * Font for the stats of the game (time left, time spent).
     */
    private Font m_font;

    /**
     * Font for the results of the game.
     */
    private Font m_font2;

    /**
     * List of positions where the ship has been located. Used to draw the ship's trajectory.
     */
    private LinkedList<Vector2d> m_positions;

    //Colors:
    //Paper easy-read format:
    /*private Color background = Color.white;
    private Color trajectory = Color.black;
    private Color obstacle = Color.darkGray;
    private Color finalResult = Color.yellow;
    private Color fontColor = Color.red;  */

    //Execution format:
    private Color background = Color.black;
    private Color trajectory = Color.white;
    private Color obstacle = Color.darkGray;
    private Color finalResult = Color.yellow;
    private Color fontColor = Color.MAGENTA;

    /**
     * Signals the first time the game is pained.
     */
    private boolean m_firstDraw;

    /**
     * Buffer for the image of the map.
     */
    private BufferedImage m_mapImage;

    /**
     * Controller, used for debug printing.
     */
    private Controller m_controller;

    /**
     * Constructor of the class.
     * @param a_game Game to paint.
     * @param a_size Size of the map.
     * @param a_map Map to be painted.
     * @param a_ship Ship of the game.
     * @param a_controller Controller of the ship.              
     */
    public PTSPView(Game a_game, Dimension a_size, Map a_map, Ship a_ship, Controller a_controller) {
        m_game = a_game;
        m_map = a_map;
        m_size = a_size;
        m_ship = a_ship;
        m_font = new Font("Courier", Font.PLAIN, 16);
        m_font2 = new Font("Courier", Font.BOLD, 18);
        m_positions = new LinkedList<Vector2d>();
        m_firstDraw = true;
        m_mapImage = null;
        m_controller = a_controller;
    }

    /**
     * Main method to paint the game
     * @param gx Graphics object.
     */
    public void paintComponent(Graphics gx)
    {
        Graphics2D g = (Graphics2D) gx;

        //For a better graphics, enable this: (be aware this could bring performance issues depending on your HW & OS).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(background);
        g.fillRect(0, 0, m_size.width, m_size.height);

        //Draw the map.
        if(m_firstDraw)
        {
            //Copy to the buffer only the first time the game is drawn.
            m_mapImage = new BufferedImage(m_size.width, m_size.height,BufferedImage.TYPE_INT_RGB);
            Graphics2D gImage = m_mapImage.createGraphics();
            gImage.setColor(background);
            gImage.fillRect(0, 0, m_size.width, m_size.height);
            for(int i = 0; i < m_map.getMapChar().length; ++i)
            {
                for(int j = 0; j < m_map.getMapChar()[i].length; ++j)
                {
                    if(m_map.isObstacle(i,j))
                    {
                        gImage.setColor(obstacle);
                        gImage.fillRect(i,j,1,1);
                    }
                }
            }
            m_firstDraw = false;

        } else {
            //Just paint the buffer from the 2nd time on.
            g.drawImage(m_mapImage,0,0,null);
        }

        //Paint all objects of the game.
        synchronized (Game.class)
        {
            for(Waypoint p : m_game.getWaypoints())
                p.draw(g);

            m_game.getShip().draw(g);
        }

        //Update positions to draw trajectory.
        if(m_ship.ps.x != m_ship.s.x || m_ship.ps.y != m_ship.s.y)
        {
            m_positions.add(m_ship.s.copy());
        }

        //Draw the trajectory
        g.setColor(trajectory);
        Vector2d oldPos = null;
        for(Vector2d pos : m_positions)
        {
            if(oldPos == null)
            {
                oldPos = pos;
            }else
            {
                g.drawLine((int)Math.round(oldPos.x),(int)Math.round(oldPos.y),(int)Math.round(pos.x),(int)Math.round(pos.y));
                oldPos = pos;
            }
        }
        
        if(m_controller != null)
            m_controller.paint(g);

        //Paint stats of the m_game.
        paintStats(g);
    }

    /**
     * Paints texts on the game, as the total and time left, and results.
     * @param g Graphics device.
     */
    private void paintStats(Graphics2D g)
    {
        g.setColor(fontColor);
        g.setFont(m_font);
        g.drawString("Total time: " + m_game.getTotalTime(), 10, 20);
        g.drawString("Time left: " + m_game.getStepsLeft(), 10, 40);

        //Last action
        /*if(m_ship.getActionList().size() > 0)
        {
            g.setColor(Color.cyan);
            g.setFont(m_font2);
            int actionId = (Integer) m_ship.getActionList().get(m_ship.getActionList().size()-1);
            if( framework.core.Action.getTurning(actionId) == -1)
                g.drawString("LEFT", 10, 80);
            if( framework.core.Action.getThrust(actionId))
                g.drawString("THRUST", 70, 100);
            if(framework.core.Action.getTurning(actionId)  == 1)
                g.drawString("RIGHT", 160, 100);
        }  */


        //Draw the results if the game is over.
        if(m_game.getWaypointsLeft() == 0)
        {
            g.setColor(finalResult);
            g.setFont(m_font2);
            g.drawString("Final score: " + m_game.getWaypointsVisited() +
                    " waypoints in " + m_game.getTotalTime() + " steps.", 10, 80);
        }else if(m_game.getStepsLeft() <= 0)
        {
            g.setColor(finalResult);
            g.setFont(m_font2);
            g.drawString("Time out. Final score: " + m_game.getWaypointsVisited() +
                    " waypoints in " + m_game.getTotalTime() + " steps.", 10, 100);
        }
    }

    /**
     * Gets the dimensions of the window.
     * @return the dimensions of the window.
     */
    public Dimension getPreferredSize() {
        return m_size;
    }

}
