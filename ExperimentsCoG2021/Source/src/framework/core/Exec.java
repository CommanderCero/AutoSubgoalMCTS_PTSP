package framework.core;

import controllers.keycontroller.KeyController;
import framework.core.*;
import framework.utils.File2String;
import framework.utils.Vector2d;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class Exec
{
    /**
     *  Name of the map to play in.
     */
    public static String[] m_mapNames = null;

    /**
     * Name of the file that contains the action of a game that we wan to see the replay of.
     */
    public static String m_actionFilename = null;

    /**
     * Name of the controller class to execute, including full package.
     */
    public static String m_controllerName = null;

    /**
     *  Indicates if the actions must be saved to a file.
     */
    public static boolean m_writeOutput = false;

    /**
     * Indicates if the graphics are enabled for the executmmion.
     */
    public static boolean m_visibility = true;

    /**
     * Instance of the game.
     */
    public static Game m_game = null;

    /**
     * Instance of the controller to execute.
     */
    public static Controller m_controller = null;

    /**
     * Graphics object in charge of painting the game.
     */
    public static PTSPView m_view = null;

    /**
     * Indicates of execution output is enabled.
     */
    public static boolean m_verbose = false;


    /**
     * Instantiates the controller, using m_controllerName
     * @return if the controller could be created
     */
    protected static boolean instanceController()
    {
        boolean ok = true;
        if(m_controllerName == null || m_controllerName.equalsIgnoreCase("controllers.keycontroller.KeyController") )
        {
            //Default, or specified, create the keycontroller.
            m_controller = new KeyController();
            m_controllerName = "controllers.keycontroller.KeyController";
        }else
        {
            ok = createController();
            if(ok)
            {
                m_game.go();
                m_game.getShip().setStarted(true);
            }
        }
        return ok;

    }

    /**
     * Creates a new controller with the given name.
     * @return true if it could be created.
     */
    protected static boolean createController()
    {
       boolean created = false;
        try
        {
            Class controllerClass = Class.forName(m_controllerName);
            Class[] gameArgClass = new Class[]{Game.class, long.class};
            Constructor controllerArgsConstructor = controllerClass.getConstructor(gameArgClass);

            //Create the controller.
            created = initializeController(controllerArgsConstructor,m_controllerName);

        }catch(NoSuchMethodException e)
        {
            System.err.println("Constructor " + m_controllerName + "(Game,long) not found in controller class:");
            e.printStackTrace();
        }catch(ClassNotFoundException e)
        {
            System.err.println("Class " + m_controllerName + " not found for the controller:");
            e.printStackTrace();
        }

        return created;
    }

    /**
     *  Initializes the controller. Takes into account the initialization time.
     * @param a_cons Constructor of the controller.
     * @param a_contName Name of the controller to instantiate.
     * @return true if controller could be initialized.
     */
    protected static boolean initializeController(Constructor a_cons, String a_contName)
    {
        long startingTime = System.currentTimeMillis();
        long finalDateMs = startingTime + PTSPConstants.INIT_TIME_MS;
        boolean ok = false;

        try
        {
            Object[] constructorArgs = new Object[] {m_game.getCopy(), finalDateMs};
            m_controller = (Controller) a_cons.newInstance(constructorArgs);

            long timeAfter = System.currentTimeMillis();
            if(timeAfter > finalDateMs)
            {
                if(m_verbose) System.out.println("Controller initialization time out (" + (PTSPConstants.INIT_TIME_MS + timeAfter - finalDateMs) + " ms).");
            }
            else
            {
                ok = true;
                if(m_verbose) System.out.println("Controller initialization time: " + (PTSPConstants.INIT_TIME_MS + timeAfter - finalDateMs) + " ms.");
            }
        }catch(InstantiationException e)
        {
            System.err.println("Exception instantiating " + a_contName + ":");
            e.printStackTrace();
        }catch(IllegalAccessException e)
        {
            System.err.println("Illegal access exception when instantiating " + a_contName + ":");
            e.printStackTrace();
        }catch(InvocationTargetException e)
        {
            System.err.println("Exception calling the constructor " + a_contName + "(Game):");
            e.printStackTrace();
        }

        return ok;
    }

    /**
     * Creates a game and the controller.
     * @return true if the game is ready to be played.
     */
    protected static boolean prepareGame()
    {
        //Create the game instance.
        m_game = new Game(m_mapNames);
        //and the controller
        return instanceController();
    }

    /**
     * Waits until the next step.
     * @param duration Amount of time to wait for.
     */
    protected static void waitStep(int duration) {

        try
        {
            Thread.sleep(duration);
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    protected static boolean runFromData( char[][] map, Vector2d startingPoint, LinkedList<Vector2d> wayPoints)
    {
        //Create the game instance.
        m_game = new Game(map, startingPoint, wayPoints);

        //and the controller
        return instanceController();
    }



    /**
     * Writes a map to the given filename.
     */
    protected static void writeMap(Map map, String filename)
    {
        StringBuffer sb = new StringBuffer();
        char[][] grid = map.getMapChar();
        String[] lines = new String[grid[0].length+4];

        lines[0] = "type octile";
        lines[1] = "height " + grid[0].length;
        lines[2] = "width " + grid.length;
        lines[3] = "map";

        for(int i = 0; i < grid[0].length; i++)
        {
            for(int j = 0; j < grid.length; ++j)
            {
                char data = grid[j][i];
                sb.append(data);
            }
            lines[i+4] = sb.toString();
            sb = new StringBuffer();
        }

        File2String.put(lines, filename);
    }

    /**
     * Reads the forces from a file and returns them in an array.
     * @param a_filename Name of the file where the forces are.
     * @return the array of forces to apply.
     * @throws Exception from file handling.
     */
    protected static int[] readForces(String a_filename) throws Exception {
        BufferedReader in = new BufferedReader(new FileReader(a_filename));
        String line = in.readLine().trim();
        int n = Integer.parseInt(line);
        int[] f = new int[n];
        for (int i=0; i<n; i++) {
            line = in.readLine().trim();
            f[i] = Integer.parseInt(line);
        }
        in.close();
        return f;
    }


}