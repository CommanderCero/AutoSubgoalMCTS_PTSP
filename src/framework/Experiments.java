package framework;

import controllers.autoSubgoalMCTS.AbstractController;
import controllers.autoSubgoalMCTS.AutoSubgoalController;
import controllers.autoSubgoalMCTS.BehaviourFunctions.PositionBehaviourFunction;
import controllers.autoSubgoalMCTS.MyMCTSController;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalPredicates.PositionGridPredicate;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.MCTSNoveltySearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.RandomPredicateSearch.RandomPredicateSearch;
import controllers.keycontroller.KeyController;
import framework.core.Exec;
import framework.core.Game;
import framework.core.PTSPConstants;
import framework.core.PTSPView;
import framework.utils.JEasyFrame;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class Experiments extends Exec
{
    /**
     * For running multiple games without visuals, in several maps (m_mapNames)
     * Running many games and looking at the average score (and standard deviation/error) helps to get a better
     * idea of how well the controller is likely to do in the competition. It waits until the controller responds.
     *
     * @param trials The number of trials to be executed
     */
    public static void runExperiments(int trials, String experimentName) throws IOException
    {
        // Prepare file writer
        Format formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
        String dateString = formatter.format(new Date(System.currentTimeMillis()));
        String csvPath = String.format("%s_%s.csv", experimentName, dateString);
        try(CSVWriter writer = new CSVWriter(csvPath, new String[]{"Trial", "MapName", "WaypointsVisited", "Steps", "FmCallsPerAction", "TimePerActionMs"}))
        {
            //Prepare the average results.
            double avgTotalWaypoints=0;
            double avgTotalTimeSpent=0;
            int totalDisqualifications=0;
            int totalNumGamesPlayed=0;
            boolean moreMaps = true;

            for(int m = 0; moreMaps && m < m_mapNames.length; ++m)
            {
                String mapName = m_mapNames[m];
                double avgWaypoints=0;
                double avgTimeSpent=0;
                int numGamesPlayed = 0;

                if(m_verbose)
                {
                    System.out.println("--------");
                    System.out.println("Running " + m_controllerName + " in map " + mapName + "...");
                }

                //For each trial...
                for(int i=0;i<trials;i++)
                {
                    System.out.println("Running trial " + i + "...");
                    // ... create a new game.
                    if(!prepareGame())
                        continue;

                    numGamesPlayed++; //another game

                    //Play the game until the end.
                    long fmCallsSum = 0;
                    long msPerActionSum = 0;
                    while(!m_game.isEnded())
                    {
                        Game copy = m_game.getCopy();

                        // Compute action
                        long begin = System.currentTimeMillis();
                        int action = m_controller.getAction(copy, begin + PTSPConstants.ACTION_TIME_MS);
                        long end = System.currentTimeMillis();

                        //Advance the game.
                        m_game.tick(action);

                        // Track stats
                        fmCallsSum += RewardGame.getCalls();
                        msPerActionSum += end - begin;
                    }

                    //Update the averages with the results of this trial.
                    avgWaypoints += m_game.getWaypointsVisited();
                    avgTimeSpent += m_game.getTotalTime();

                    //Print the results.
                    if(m_verbose)
                    {
                        System.out.print(i+"\t");
                        m_game.printResults();
                    }

                    // Save the route
                    File f = new File(mapName);
                    String routePath = String.format("routeData_%s_%s_trial_%d.txt", experimentName, f.getName().substring(0, f.getName().lastIndexOf('.')), i);
                    m_game.saveRoute(routePath);

                    // Save stats to csv file
                    writer.append(i + 1);
                    writer.append(mapName);
                    writer.append(m_game.getWaypointsVisited());
                    writer.append(m_game.getTotalTime());
                    writer.append(fmCallsSum / m_game.getTotalTime());
                    writer.append(msPerActionSum / m_game.getTotalTime());
                }

                moreMaps = m_game.advanceMap();

                avgTotalWaypoints += (avgWaypoints / numGamesPlayed);
                avgTotalTimeSpent += (avgTimeSpent / numGamesPlayed);
                totalDisqualifications += (trials - numGamesPlayed);
                totalNumGamesPlayed += numGamesPlayed;

                //Print the average score.
                if(m_verbose)
                {
                    System.out.println("--------");
                    System.out.format("Average waypoints: %.3f, average time spent: %.3f\n", (avgWaypoints / numGamesPlayed), (avgTimeSpent / numGamesPlayed));
                    System.out.println("Disqualifications: " + (trials - numGamesPlayed) + "/" + trials);
                }
            }

            //Print the average score.
            if(m_verbose)
            {
                System.out.println("-------- Final score --------");
                System.out.format("Average waypoints: %.3f, average time spent: %.3f\n", (avgTotalWaypoints / m_mapNames.length), (avgTotalTimeSpent / m_mapNames.length));
                System.out.println("Disqualifications: " + (trials*m_mapNames.length - totalNumGamesPlayed) + "/" + trials*m_mapNames.length);
            }
        }
    }

    public static void main(String[] args) throws IOException
    {
        m_mapNames = new String[]{"maps/StageA/ptsp_map01.map", "maps/StageA/ptsp_map02.map",
                "maps/StageA/ptsp_map08.map", "maps/StageA/ptsp_map19.map",
                "maps/StageA/ptsp_map24.map", "maps/StageA/ptsp_map35.map",
                "maps/StageA/ptsp_map40.map", "maps/StageA/ptsp_map45.map",
                "maps/StageA/ptsp_map56.map", "maps/StageA/ptsp_map61.map"};

        // Setup - !Each controller tested inherits from the AbstractController!
        AbstractController.stopCondition = AbstractController.StopCondition.ForwardCalls;
        AbstractController.maxForwardCalls = 70000; // Is equal to around 40ms for the Vanilla MCTS algorithm
        AbstractController.rng.setSeed(0); // Ensure deterministic results, only deterministic if ForwardCalls are used as stop condition

        int numTrials=10;
        m_verbose = true; // Print debug info

        // Vanilla MCTS
        //m_controllerName = "controllers.autoSubgoalMCTS.MyMCTSController";
        //runExperiments(numTrials, "VanillaMCTS");

        // Original Sugoal MCTS algorithm
        //m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController";
        //PositionGridPredicate predicate = new PositionGridPredicate(20, 3);
        //RandomPredicateSearch.treatHorizonStatesAsSubgoals = true;
        //AutoSubgoalController.subgoalSearch = new RandomPredicateSearch(predicate, 4, 400, AbstractController.rng);
        //runExperiments(numTrials, "S-MCTS");

        // Modified Subgoal MCTS algorithm - Does not treat every horizon-state as a subgoal
        //m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController";
        //PositionGridPredicate predicate = new PositionGridPredicate(20, 3);
        //RandomPredicateSearch.treatHorizonStatesAsSubgoals = false;
        //AutoSubgoalController.subgoalSearch = new RandomPredicateSearch(predicate, 4, 400, AbstractController.rng);
        //runExperiments(numTrials, "MS-Subgoal-MCTS");

        // Quality Diversity Subgoal MCTS algorithm - The algorithm proposed by us
        //m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController";
        //AutoSubgoalController.subgoalSearch = new MCTSNoveltySearch(4, new PositionBehaviourFunction(), AbstractController.rng);
        //runExperiments(numTrials, "QD-Subgoal-MCTS");

        // Vanilla Genetic algorithm
        m_controllerName = "controllers.autoSubgoalMCTS.GeneticAlgorithm.GAController";
        runExperiments(numTrials, "VanillaGA");
    }
}