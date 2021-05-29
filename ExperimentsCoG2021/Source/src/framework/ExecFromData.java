package framework;

import controllers.lineofsight.LineOfSight;
import controllers.random.RandomController;
import framework.core.*;
import framework.utils.JEasyFrame;
import framework.utils.Vector2d;

import java.awt.*;
import java.util.LinkedList;

/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class ExecFromData extends Exec
{

    /**
     * Run the game in asynchronous mode but proceed as soon as the controllers reply. The time limit still applies so
     * so the game will proceed after 40ms regardless of whether the controller managed to calculate a turn.
     * The map is supplied in data structure, rather than read from a file. It executes only once in one map.
     *
     * @param fixedTime Whether or not to wait until PTSPConstants.ACTION_TIME_MS are up even if the controller already responded
     * @param visual Indicates whether or not to use visuals
     * @param map Map contents (just obstacles and free spaces).
     * @param startingPoint Starting point of the ship.
     * @param wayPoints List of waypoint positions.
     */
    public static void runGameTimedSpeedOptimisedData(boolean fixedTime,boolean visual, char[][] map, Vector2d startingPoint, LinkedList<Vector2d> wayPoints)
    {
        //Get the game ready.
        if(!runFromData(map, startingPoint, wayPoints))
            return;

        //Indicate what are we running
        if(m_verbose) System.out.println("Running " + m_controllerName + " in map created from data...");

        JEasyFrame frame;
        if(visual)
        {
            //View of the game, if applicable.
            m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
            frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controllerName);
        }

        //Start the controller in a separated thread.
        Thread thr = new Thread(m_controller);
        thr.start();


        while(!m_game.isEnded() && thr.isAlive())
        {
            //When the result is expected:
            long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

            //Supply the game copy and the expected time
            m_controller.update(m_game.getCopy(),due);

            int waited=PTSPConstants.ACTION_TIME_MS/PTSPConstants.INTERVAL_WAIT;

            for(int j=0;j<PTSPConstants.ACTION_TIME_MS/PTSPConstants.INTERVAL_WAIT;j++)
            {
                waitStep(PTSPConstants.INTERVAL_WAIT);

                if(m_controller.hasComputed())
                {
                    waited=j;
                    break;
                }
            }

            if(fixedTime)
                waitStep(((PTSPConstants.ACTION_TIME_MS/PTSPConstants.INTERVAL_WAIT)-waited)*PTSPConstants.INTERVAL_WAIT);

            //Advance the game
            m_game.tick(m_controller.getMove());

            //And paint everything.
            if(visual)
            {
                m_view.repaint();
            }
        }

        //Print results.
        if(m_verbose)
            m_game.printResults();

        //And save the route, if requested:
        if(m_writeOutput)
            m_game.saveRoute();

        //Finish the controller.
        m_controller.terminate();
    }


    /**
     * Runs a game from data, receiving the controller by parameter. Returns the game when finished.
     * @param controller  Controller to run. <b> Important: </b> It must be created from outside: it does not
     *                    check that the initialization time is respected!
     * @param visual      Whether to show graphics or not.
     * @param accelerated Whether the game must be accelerated or not.
     * @param map         Data grid with the map information (free and obstacle spaces).
     * @param startingPoint Starting point of the ship.
     * @param wayPoints   List with the positions of the waypoints.
     * @return the instance of the game played.
     */
    public static Game runGame(Controller controller, boolean visual, boolean accelerated,
                               char[][] map, Vector2d startingPoint, LinkedList<Vector2d> wayPoints)
    {
        m_controller = controller;
        controller.reset();

        //Create the game instance.
        m_game = new Game(map, startingPoint, wayPoints);
        m_game.go();
        m_game.getShip().setStarted(true);

        JEasyFrame frame = null;
        if(visual)
        {
            //View of the game, if applicable.
            m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
            frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controller.toString());
        }

        while(!m_game.isEnded())
        {
            //When the result is expected:
            long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

            //Advance the game.
            m_game.tick(m_controller.getAction(m_game.getCopy(), due));

            if(!accelerated)
                waitStep(10);

            //And paint everything.
            if(visual)
            {
                m_view.repaint();
            }
        }

        //Print results.
        if(m_verbose)
            m_game.printResults();

        //And save the route, if requested:
        if(m_writeOutput)
            m_game.saveRoute();

        return m_game;

    }

    /**
     * Runs a game from file (in m_mapNames), receiving the controller by parameter. Returns the game when finished.
     * @param controller  Controller to run. <b> Important: </b> It must be created from outside: it does not
     *                    check that the initialization time is respected!
     * @param visual      Whether to show graphics or not.
     * @param accelerated Whether the game must be accelerated or not.
     * @return the instance of the game played.
     */
    public static Game runGameFromFile(Controller controller, boolean visual, boolean accelerated)
    {
        m_controller = controller;
        controller.reset();

        //Create the game instance.
        m_game = new Game(m_mapNames);
        m_game.go();
        m_game.getShip().setStarted(true);

        JEasyFrame frame = null;
        if(visual)
        {
            //View of the game, if applicable.
            m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
            frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controller.toString());
        }


        while(!m_game.isEnded())
        {
            //When the result is expected:
            long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

            //Advance the game.
            m_game.tick(m_controller.getAction(m_game.getCopy(), due));

            if(!accelerated)
                waitStep(10);

            //And paint everything.
            if(visual)
            {
                m_view.repaint();
            }
        }

        //Print results.
        if(m_verbose)
            m_game.printResults();

        //And save the route, if requested:
        if(m_writeOutput)
            m_game.saveRoute();


        return m_game;

    }


    /**
     * Runs a game from file (in m_mapNames), receiving the controller by parameter. Returns the game when finished.
     * @param controller  Controller to run. <b> Important: </b> It must be created from outside: it does not
     *                    check that the initialization time is respected!
     * @param a_game The game to be played with.
     * @param visual      Whether to show graphics or not.
     * @param accelerated Whether the game must be accelerated or not.
     * @return the instance of the game played.
     */
    public static Game runGameFromGame(Controller controller, Game a_game, boolean visual, boolean accelerated)
    {
        m_controller = controller;
        controller.reset();

        //Create the game instance.
        a_game.go();
        a_game.getShip().setStarted(true);

        JEasyFrame frame = null;
        if(visual)
        {
            //View of the game, if applicable.
            m_view = new PTSPView(a_game, a_game.getMapSize(), a_game.getMap(), a_game.getShip(), m_controller);
            frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controller.toString());
        }


        while(!a_game.isEnded())
        {
            //When the result is expected:
            long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

            //Advance the game.
            a_game.tick(m_controller.getAction(a_game.getCopy(), due));

            if(!accelerated)
                waitStep(10);

            //And paint everything.
            if(visual)
            {
                m_view.repaint();
            }
        }

        //Print results.
        if(m_verbose)
            a_game.printResults();

        //And save the route, if requested:
        if(m_writeOutput)
            a_game.saveRoute();


        return a_game;

    }



    /**
     * The main method. Several options are listed - simply remove comments to use the option you want.
     *
     * @param args the command line arguments. Not needed in this class.
     */
    public static void main(String[] args)
    {

        m_mapNames = new String[]{"maps/StageA/ptsp_map01.map"};  //Set here the name of the map to play in.
        m_controllerName = "controllers.greedy.GreedyController"; //Set here the controller name. Leave it to null to play with KeyController.
        m_controllerName = "controllers.random.RandomController";
        m_visibility = true; //Set here if the graphics must be displayed or not (for those modes where graphics are allowed).
        m_writeOutput = false; //Indicate if the actions must be saved to a file after the end of the game.
        m_verbose = false;


        /////// 1. Run from data: Create the map from data structures and execute once on it.
        char [][]map = new char[500][500];
        for(int i = 0; i < map.length; ++i)
           for(int j = 0; j < map[0].length; ++j)
            {
                map[i][j] = (Math.random() > 1.95) ? Map.EDGE : Map.NIL;
            }
        Vector2d startingPoint = new Vector2d(250,450);
        LinkedList<Vector2d> waypoints = new LinkedList<Vector2d>();
        waypoints.add(new Vector2d(230,25)); waypoints.add(new Vector2d(270,75));
        waypoints.add(new Vector2d(230,100)); waypoints.add(new Vector2d(270,150));
        waypoints.add(new Vector2d(230,175)); waypoints.add(new Vector2d(270,225));
        waypoints.add(new Vector2d(230,250)); waypoints.add(new Vector2d(270,300));
        waypoints.add(new Vector2d(230,325)); waypoints.add(new Vector2d(270,375));
        boolean fixedTime=false;
        runGameTimedSpeedOptimisedData(fixedTime, m_visibility, map, startingPoint, waypoints);


        /////// 2. Execute from data, providing also a controller already created.
        /*char [][]map = new char[500][500];
        for(int i = 0; i < map.length; ++i)
            for(int j = 0; j < map[0].length; ++j)
            {
                map[i][j] = (Math.random() > 0.995) ? Map.EDGE : Map.NIL;
            }
        Vector2d startingPoint = new Vector2d(200,200);
        LinkedList<Vector2d> waypoints = new LinkedList<Vector2d>();
        waypoints.add(new Vector2d(150,150)); waypoints.add(new Vector2d(150,215));
        waypoints.add(new Vector2d(180,150)); waypoints.add(new Vector2d(300,200));
        waypoints.add(new Vector2d(215,200)); waypoints.add(new Vector2d(200,225));
        waypoints.add(new Vector2d(215,150)); waypoints.add(new Vector2d(300,300));
        boolean accelerate = true;
        m_visibility = true;

        Game gamePlayed = null;
        long now = System.currentTimeMillis();
        for(int i = 0; i < 10; ++i)
            gamePlayed = ExecFromData.runGame(new RandomController(null), m_visibility, accelerate, map, startingPoint, waypoints);

        long then = System.currentTimeMillis();
        System.out.println("Spent: " + (then-now));*/

        
        /////// 3. Execute from file, providing a controller already created. It can execute in several maps (m_mapNames).
        /*boolean accelerate = false;
        Game gamePlayed = null;
        long now = System.currentTimeMillis();
        for(int i = 0; i < 5; ++i)
        {
            gamePlayed= ExecFromData.runGameFromFile(new RandomController(null, 0), m_visibility, accelerate);
            //System.out.println("Waypoints: " + gamePlayed.getWaypointsVisited() + ", time: " + gamePlayed.getTotalTime());
        }   */
          /*
        gamePlayed.advanceMap(); //Call advance map to go to the next map in m_mapNames

        for(int i = 0; i < 5; ++i)
        {
            gamePlayed= ExecFromData.runGameFromFile(new RandomController(null), m_visibility, accelerate);
            //System.out.println("Waypoints: " + gamePlayed.getWaypointsVisited() + ", time: " + gamePlayed.getTotalTime());
        }
        long then = System.currentTimeMillis();
        System.out.println("Spent: " + (then-now));  */



        /////////4. Run from Game.
        /*char [][]charMap = new char[500][500];
        for(int i = 0; i < charMap.length; ++i)
            for(int j = 0; j < charMap[0].length; ++j)
            {
                charMap[i][j] = (Math.random() > 0.995) ? Map.EDGE : Map.NIL;
            }
        Vector2d startingPoint = new Vector2d(200,200);
        LinkedList<Vector2d> waypoints = new LinkedList<Vector2d>();
        waypoints.add(new Vector2d(150,150)); waypoints.add(new Vector2d(150,215));
        waypoints.add(new Vector2d(180,150)); waypoints.add(new Vector2d(300,200));
        waypoints.add(new Vector2d(215,200)); waypoints.add(new Vector2d(200,225));
        waypoints.add(new Vector2d(215,150)); waypoints.add(new Vector2d(300,300));
        boolean accelerate = true;

        Game game = new Game(charMap, startingPoint, waypoints);
        Game losGame = game.getCopy();
        Controller los = new LineOfSight(losGame, System.currentTimeMillis() + PTSPConstants.INIT_TIME_MS);
        losGame = ExecFromData.runGameFromGame(los, losGame, m_visibility, !m_visibility);
        double losWaypoints = losGame.getWaypointsVisited();*/
    }


}