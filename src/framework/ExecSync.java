package framework;

import controllers.autoSubgoalMCTS.AbstractController;
import controllers.autoSubgoalMCTS.AutoSubgoalController;
import controllers.autoSubgoalMCTS.BehaviourFunctions.PositionBehaviourFunction;
import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.MCTSNoveltySearch;
import controllers.autoSubgoalMCTS.VanillaMCTS;
import controllers.keycontroller.KeyController;
import framework.core.*;
import framework.utils.JEasyFrame;

import java.awt.*;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class ExecSync extends Exec
{
    /**
     * Run a game in : the game waits ONE map. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used.
     * Use this mode to play the game with the KeyController.
     *
     * @param visual Indicates whether or not to use visuals
     * @param delay The delay between time-steps
     */
    public static void runGame(boolean visual,int delay)
    {
        //Get the game ready.
        if(!prepareGame(m_mapNames[0]))
            return;

        //Indicate what are we running
        if(m_verbose) System.out.println("Running " + m_controllerName + " in map " + m_game.getMap().getFilename() + "...");

        JEasyFrame frame;
        if(visual)
        {
            //View of the game, if applicable.
            m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
            frame = new JEasyFrame(m_view, "PTSP-Game: " + m_controllerName);

            //If we are going to play the game with the cursor keys, add the listener for that.
            if(m_controller instanceof KeyController)
            {
                frame.addKeyListener(((KeyController)m_controller).getInput());
            }
        }

        while(!m_game.isEnded())
        {

            //When the result is expected:
            long due = System.currentTimeMillis()+PTSPConstants.ACTION_TIME_MS;

            //Advance the game.
            int action = m_controller.getAction(m_game.getCopy(), due);
            if(System.currentTimeMillis() > due)
            {
                //System.out.println("Controller overtimed.");
            }

            m_game.tickRandom(action);

            //Wait until de next cycle.
            waitStep(delay);

            //And paint everything.
            if(m_visibility)
            {
                m_view.repaint();
            }
        }

        if(m_verbose)
            m_game.printResults();

        //And save the route, if requested:
        if(m_writeOutput)
            m_game.saveRoute();

    }


    /**
     * The main method. Several options are listed - simply remove comments to use the option you want.
     *
     * @param args the command line arguments. Not needed in this class.
     */
    public static void main(String[] args) throws IOException
    {
        m_mapNames = new String[]{"maps/StageA/ptsp_map01.map", "maps/StageA/ptsp_map02.map",
                "maps/StageA/ptsp_map08.map", "maps/StageA/ptsp_map19.map",
                "maps/StageA/ptsp_map24.map", "maps/StageA/ptsp_map35.map",
                "maps/StageA/ptsp_map40.map", "maps/StageA/ptsp_map45.map",
                "maps/StageA/ptsp_map56.map", "maps/StageA/ptsp_map61.map"};  //Set here the name of the map to play in.

        //m_mapNames = new String[]{"test2.txt"};

        //m_controllerName = "controllers.mcts.MCTSController";
        //m_controllerName = "controllers.greedy.GreedyController"; //Set here the controller name. Leave it to null to play with KeyController.
        //m_controllerName = "controllers.mcts.MCTSController"; //Set here the controller name. Leave it to null to play with KeyController.
        //m_controllerName = "controllers.simpleGA.GAController"; //Set here the controller name. Leave it to null to play with KeyController.
        m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController"; //Set here the controller name. Leave it to null to play with KeyController.
        //m_controllerName = "controllers.autoSubgoalMCTS.VanillaMCTS"; //Set here the controller name. Leave it to null to play with KeyController.
        //m_controllerName = "controllers.autoSubgoalMCTS.GeneticAlgorithm.GAController"; //Set here the controller name. Leave it to null to play with KeyController.

        m_visibility = true; //Set here if the graphics must be displayed or not (for those modes where graphics are allowed).
        m_writeOutput = false; //Indicate if the actions must be saved to a file after the end of the game.
        m_verbose = true;

        /////// 1. Run experiments
        int numTrials=10;
        //m_controllerName = "controllers.random.RandomController";
        //runExperiments(numTrials, "Random");
        //m_controllerName = "controllers.mcts.MCTSController";
        //runExperiments(numTrials, "MCTS");
        //m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController";
        //runExperiments(numTrials, "AutoSubgoalMCTS");
        //m_controllerName = "controllers.autoSubgoalMCTS.VanillaMCTS";
        //runExperiments(numTrials, "NaiveMCTS");

        /////// 2. Runs once in a map, supplying frame rate:
        int delay = 5;  //0 or 2: quickest, PTSPConstants.DELAY: human play speed, PTSPConstants.ACTION_TIME_MS: max. controller delay
        runGame(m_visibility,delay);

        /////// 3. Runs once in a map, for the player
        /////// Use also this mode to play with the key controller.
        /////// Note: using a delay of 0 with m_visibility=true will let you see close to nothing. We recommend delay=2 instead.
        //m_controllerName = "controllers.keycontroller.KeyController";  //Use this controller to play with the KeyController.
        //int delay = PTSPConstants.DELAY;
        //runGame(m_visibility,delay);
    }


}