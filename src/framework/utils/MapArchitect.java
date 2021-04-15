package framework.utils;

import framework.core.Game;
import framework.core.Map;
import framework.core.Ship;
import framework.core.Waypoint;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.LinkedList;
import javax.swing.*;

@SuppressWarnings("serial")
/**
 * Helper class to draw maps for the PTSP.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class MapArchitect extends JComponent implements MouseListener, MouseMotionListener
{
    /**
     * Empty spot in the map.
     */
	public static char EMPTY=Map.NIL;

    /**
     * Array of pixels of the map.
     */
	private static char[][] m_grid;

    /**
     * Filename to write to.
     */
	private static String m_fileName;

    /**
     * Map to be modified.
     */
    private Map m_map;

    /**
     * Window frame reference.
     */
	private ArchitectFrame frame;

    /**
     * Waypoint locations.
     */
    private LinkedList<Vector2d> m_waypoints;

    /**
     * Start point for the ship of the game.
     */
    private Vector2d m_startPoint;

    /**
     * Maximum number of waypoints that can be put in the map.
     */
    private int m_maxNumWaypoints;

    /**
     * Size of the brush to paint obstacles and empty spaces.
     */
    private int m_brush;

    /**
     * Current position of the mouse.
     */
    private Vector2d m_mousePosition;

    /**
     * Constructor
     * @param a_fileName Filename to write to.
     * @param a_map Input map instance.
     */
    public MapArchitect(String a_fileName, Map a_map, Game a_game)
    {
        m_fileName=a_fileName;
        m_map = a_map;
        m_waypoints = new LinkedList<Vector2d>();
        m_maxNumWaypoints = 50;
        m_brush = 4;
        m_startPoint = null;
        m_mousePosition = new Vector2d();

        try
        {
        	loadMap();
            for(int i = 0; i < a_game.getWaypoints().size() && m_waypoints.size() < m_maxNumWaypoints; ++i)
            {
                m_waypoints.add((a_game.getWaypoints().get(i)).s.copy());
            }
            m_startPoint = m_map.getStartingPoint().copy();
            if(m_startPoint.x == 0 && m_startPoint.y == 0)
                m_startPoint = null;
        }
        catch(Exception e)
        {
        	System.out.println("It will create new file (" + e + ")");

            this.m_grid =new char[m_grid.length][m_grid[0].length];

            for(int i=0;i< m_grid.length;i++)
            	for(int j=0;j< m_grid[i].length;j++)
            		m_grid[i][j]=EMPTY;
        }

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * Constructor for empty maps (not read from file).
     * @param a_fileName Filename to write to.
     * @param rows Number of rows of the map.
     * @param cols Number of cols of the map.
     */
    public MapArchitect(String a_fileName, int rows, int cols)
    {
        m_fileName=a_fileName;
        m_waypoints = new LinkedList<Vector2d>();
        m_maxNumWaypoints = 10;
        m_brush = 4;
        m_startPoint = null;
        m_mousePosition = new Vector2d();

        this.m_grid =new char[rows][cols];

        for(int i=0;i< m_grid.length;i++)
            for(int j=0;j< m_grid[i].length;j++)
                m_grid[i][j]=EMPTY;

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * Paints the window.
     * @param gg Graphics device.
     */
    public void paint(Graphics gg) 
    {
        Graphics2D g=(Graphics2D)gg;

        //Grid:
        for(int i=0;i< m_grid.length;i++)
    		for(int j=0;j< m_grid[0].length;j++)
    		{
                if(isObstacle(m_grid[i][j]))
    				g.setColor(Color.BLACK);
    			else
    				g.setColor(Color.WHITE);

                g.fillRect(i,j,1,1);
    		}


        //Waypoints
        g.setColor(Color.red);
        int drawRadius = Ship.SHIP_RADIUS * Waypoint.RADIUS;
        for(Vector2d v : m_waypoints)
        {
            g.fillOval((int) (v.x - drawRadius*0.5),(int) (v.y - drawRadius*0.5),drawRadius,drawRadius);
        }

        //Start point
        if(m_startPoint != null)
        {
            g.setColor(Color.cyan);
            g.fillOval((int) (m_startPoint.x - drawRadius*0.5),(int) (m_startPoint.y - drawRadius*0.5),drawRadius-1,drawRadius-1);
        }
        
        //THIS MUST BE THE LAST THING TO BE PAINTED: Brush.
        char choice = frame.getChoice();
        if(choice == Map.NIL || choice == Map.EDGE)
        {
            int xStart = (int)m_mousePosition.x - m_brush;
            int xEnd = (int)m_mousePosition.x + m_brush;
            int yStart = (int)m_mousePosition.y - m_brush;
            int yEnd = (int)m_mousePosition.y + m_brush; 
            g.setColor(Color.blue);
            g.drawRect(xStart, yStart, xEnd-xStart, yEnd-yStart);
            //System.out.println(". Painting from (" + xStart + "," + yStart + "), w: " + (xEnd-xStart) + ", h: " + (yEnd-yStart));
        }
        
    }

    /**
     * Indicates if a given character is an obstacle or not.
     * @param data character to check.
     * @return true if it represents an obstacle.
     */
    private boolean isObstacle(char data)
    {
        if(data == '@' || data == 'T' || data == '\u0000' || data == Map.EDGE)
            return true;
        return false;
    }

    /**
     * Gets the dimensions of the screen.
     * @return The dimension of the screen.
     */
    public Dimension getPreferredSize()
    {
        return new Dimension(m_grid.length,m_grid[0].length);
    }

    /**
     * Registers an event in the application
     * @param x x position of the event.
     * @param y y position of the event.
     * @param button Button that triggers the event.
     */
    public void registerEvent(int x,int y,int button)
    {
        char choice = frame.getChoice();
        if(choice == ' ' || (button == MouseEvent.NOBUTTON))
            return;

		int row=x;
		int col=y;

		if(row<m_grid.length && col<m_grid[0].length)
        {
            if(button == MouseEvent.BUTTON1)
            {
                m_brush = frame.getBrushValue();
                if(choice == Map.EDGE)
                    setBrush(row, col, choice);
                else if(choice == Map.NIL)
                    clear(row,col);
                else if(choice == Map.WAYPOINT)
                    addWaypoint(row,col);
                else if(choice == Map.START)
                    setStart(row, col);
            }
        }
    }

    /**
     * Register a drag event.
     * @param x x position of the event.
     * @param y y position of the event.
     */
    public void registerDragEvent(int x,int y)
    {
        char choice = frame.getChoice();
        if(choice == ' ' || choice == Map.WAYPOINT)
            return;

		int row=x;
		int col=y;

		if(row<m_grid.length && col<m_grid[0].length)
        {
            m_brush = frame.getBrushValue();
            if(choice == Map.EDGE)
                setBrush(row, col, choice);
            else if(choice == Map.NIL)
                clear(row,col);
        }
    }

    /**
     * Paints on the screen when the mouse is clicked, using the brush size.
     * @param a_x x position of the click point.
     * @param a_y y position of the click point.
     * @param a_data data to write.
     */
    private void setBrush(int a_x, int a_y, char a_data)
    {
        int xStart = a_x - m_brush;
        int xEnd = a_x + m_brush;
        int yStart = a_y - m_brush;
        int yEnd = a_y + m_brush;

        for(int i = xStart; i <= xEnd; ++i)
            for(int j = yStart; j <= yEnd; ++j)
            {
                if(i > 0 && j > 0 && i < m_grid.length && j < m_grid[i].length)
                    m_grid[i][j] = a_data;
            }
    }

    /**
     * removes a waypoint from the map, if any close to the click point.
     * @param a_x x click position.
     * @param a_y y click position.
     */
    private void clear(int a_x, int a_y)
    {
        int index = clickOnWaypoint(a_x,a_y);
        if(index != -1)
        {
            Vector2d v = m_waypoints.get(index);
            m_grid[(int)v.x][(int)v.y] = Map.NIL;
            m_waypoints.remove(index);
            frame.updateNumWaypoints(m_waypoints.size());
        }
        else
        {
            setBrush(a_x,a_y,Map.NIL);
        }
    }

    /**
     * Sets the start position
     * @param a_x x coordinate
     * @param a_y y coordinate
     */
    private void setStart(int a_x, int a_y)
    {
        if(!isObstacle(m_grid[a_x][a_y]))
        {
            if(m_startPoint != null)
               m_grid[(int)m_startPoint.x][(int)m_startPoint.y] = Map.NIL;

            m_startPoint = new Vector2d(a_x,a_y);
            m_grid[(int)m_startPoint.x][(int)m_startPoint.y] = Map.START;
        }
    }

    /**
     * Adds a waypoint to the map.
     * @param a_x x position of the map
     * @param a_y y position of the map
     */
    private void addWaypoint(int a_x, int a_y)
    {
        if(!isObstacle(m_grid[a_x][a_y]) && m_waypoints.size() < m_maxNumWaypoints)
        {
            m_grid[a_x][a_y] = Map.WAYPOINT;
            m_waypoints.add(new Vector2d(a_x,a_y));
            frame.updateNumWaypoints(m_waypoints.size());
        }
    }

    /**
     * Indicates which waypoint was clicked, if any.
     * @param a_x x position of the click event.
     * @param a_y y position of the click event.
     * @return the index of the waypoint clicked, -1 if no waypoint found near the click.
     */
    private int clickOnWaypoint(int a_x, int a_y)
    {
        Vector2d click = new Vector2d(a_x,a_y);
        int drawRadius = Ship.SHIP_RADIUS * Waypoint.RADIUS;
        for(int i = 0; i < m_waypoints.size(); ++i)
        {
            Vector2d v = m_waypoints.get(i);
            if(click.dist(v) < drawRadius * 0.5)
            {
                return i;
            }

        }
        return -1;
    }

    /**
     * Registers mouse event clicks.
     * @param me mouse event.
     */
	public void mouseClicked(MouseEvent me)
	{
		registerEvent(me.getX(),me.getY(),me.getButton());
		repaint();
	}

    /**
     * Registers a drag event.
     * @param me drag mouse event.
     */
	public void mouseDragged(MouseEvent me) 
	{
		registerDragEvent(me.getX(), me.getY());
        m_mousePosition.x = me.getX();
        m_mousePosition.y = me.getY();
        m_brush = frame.getBrushValue();
		repaint();
	}

	public void mouseEntered(MouseEvent arg0){}

	public void mouseExited(MouseEvent arg0)
	{
		//save();
	}

	public void mousePressed(MouseEvent arg0){}

	public void mouseReleased(MouseEvent arg0){}

    /**
     * Register a mouse move event
     * @param me the event.
     */
	public void mouseMoved(MouseEvent me)
	{
        m_mousePosition.x = me.getX();
        m_mousePosition.y = me.getY();
        m_brush = frame.getBrushValue();
        //System.out.println(m_mousePosition.x + " " + m_mousePosition.y + " ");
        repaint();
	}

    /**
     * Saves the map to the file indicated.
     */
	public static void save()
	{
        writeMap();
	}

    /**
     * Writes a map.
     */
    private static void writeMap()
    {
        StringBuffer sb = new StringBuffer();
        String[] lines = new String[m_grid[0].length+4];

        lines[0] = "type octile";
        lines[1] = "height " + m_grid[0].length;
        lines[2] = "width " + m_grid.length;
        lines[3] = "map";

        for(int i = 0; i < m_grid[0].length; i++)
        {
            for(int j = 0; j < m_grid.length; ++j)
            {
                char data = m_grid[j][i]; //m_map.getMapChar()[j][i];
                sb.append(data);
            }
            lines[i+4] = sb.toString();
            sb = new StringBuffer();
        }

        File2String.put(lines,m_fileName);
    }

    /**
     * Loads a map in the grid.
     * @throws Exception
     */
	public void loadMap() throws Exception
	{
		m_grid = new char[m_map.getMapChar().length][m_map.getMapChar()[0].length];

        for(int i = 0; i < m_grid.length; ++i)
        {
            for(int j = 0; j < m_grid[0].length; ++j)
            {
                m_grid[i][j] = m_map.getMapChar()[i][j];
            }
        }

	}

    /**
     * Shows the window frame.
     * @return the frame.
     */
    public MapArchitect showArchitect()
    {
        frame = new ArchitectFrame(this);
        frame.updateNumWaypoints(m_waypoints.size());
        return this;
    }


    /**
     * Frame class to hold the map editor.
     */
    class ArchitectFrame extends JFrame
    {
        public MapArchitect comp;
        
        private JPanel choices,bottomPanel;
        private JRadioButton[] buttons;// wall, ship, enemy, pack;
        private ButtonGroup group;
        private JComboBox comboBrush;
        private JButton saveButton;
        private JLabel numWaypointsLabel;

        public char getChoice()
        {
        	if(buttons[0].isSelected())
                return Map.NIL;
        	else if(buttons[1].isSelected())
                return Map.EDGE;
            else if(buttons[2].isSelected())
                return Map.WAYPOINT;
            else if(buttons[3].isSelected())
                return Map.START;

        	return ' ';
        }
        
        public ArchitectFrame(MapArchitect comp)
        {
        	this.comp=comp;

            choices=new JPanel(new GridLayout(1,4));
            bottomPanel=new JPanel(new GridLayout(1,2));
            buttons=new JRadioButton[4];
            numWaypointsLabel = new JLabel();
            
            buttons[0]=new JRadioButton("Clear");
            buttons[1]=new JRadioButton("Obstacle");
            buttons[2]=new JRadioButton("Waypoint");
            buttons[3]=new JRadioButton("Start point");
            
    		group=new ButtonGroup();
    	    group.add(buttons[0]);
    	    group.add(buttons[1]);
    	    group.add(buttons[2]);
    	    group.add(buttons[3]);
    	    
    	    choices.add(buttons[0]);
    	    choices.add(buttons[1]);
    	    choices.add(buttons[2]);
    	    choices.add(buttons[3]);

            saveButton = new JButton("Save");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    //System.out.println("You clicked the button");
                    MapArchitect.save();
                }
            });
            choices.add(saveButton);

            comboBrush = new JComboBox();
            comboBrush.addItem(1);
            comboBrush.addItem(2);
            comboBrush.addItem(4);
            comboBrush.addItem(8);
            comboBrush.addItem(16);
            comboBrush.addItem(32);

            comboBrush.setSelectedIndex(3);

            bottomPanel.add(comboBrush);
            bottomPanel.add(numWaypointsLabel);

            getContentPane().add(BorderLayout.CENTER,comp);
            getContentPane().add(BorderLayout.NORTH,choices);
            getContentPane().add(BorderLayout.SOUTH,bottomPanel);
            
            pack();
            this.setVisible(true);
            this.setResizable(false);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            repaint();
        }

        public int getBrushValue()
        {
            return (Integer) comboBrush.getSelectedItem();
        }

        public void updateNumWaypoints(int a_num)
        {
            numWaypointsLabel.setText(" " + a_num + " Waypoints");
            repaint();
        }


    }

    public static void main(String args[])
    {
        if(args.length > 1)
        {
            try{
                Map map = new Map(null, args[0]);
                new MapArchitect(args[1],map, new Game(args[0])).showArchitect();
            }catch(Exception e)
            {
                new MapArchitect(args[1],500,500).showArchitect();
            }
        }else System.out.println("Include a map filename as input and output");


    }
}

