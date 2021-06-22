package framework;

import controllers.autoSubgoalMCTS.AbstractController;
import controllers.autoSubgoalMCTS.AutoSubgoalController;
import controllers.autoSubgoalMCTS.BehaviourFunctions.PositionBehaviourFunction;
import controllers.autoSubgoalMCTS.GeneticAlgorithm.GAController;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalPredicates.PositionGridPredicate;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.MCTSNoveltySearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.RandomPredicateSearch.RandomPredicateSearch;
import controllers.autoSubgoalMCTS.VanillaMCTS;
import framework.core.Exec;
import framework.core.Game;
import framework.core.PTSPConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class ExperimentSteering extends Exec
{
    /**
     * For running multiple games without visuals, in several maps (m_mapNames)
     * Running many games and looking at the average score (and standard deviation/error) helps to get a better
     * idea of how well the controller is likely to do in the competition. It waits until the controller responds.
     *
     * @param trials The number of trials to be executed
     */
    public static double runExperiments(int trials, String outputPath) throws IOException
    {
        // Prepare file writer
        ArrayList<Double> scorePerTrial = new ArrayList<>();
        String csvPath = String.format("%s/%s_%d.csv", outputPath, ALGORITHM, NUM_WAYPOINTS);
        try(CSVWriter writer = new CSVWriter(csvPath, new String[]{"Trial", "MapName", "WaypointsVisited", "Steps", "FmCallsPerAction", "TimePerActionMs"}))
        {
            //Prepare the average results.
            double avgTotalWaypoints=0;
            double avgTotalTimeSpent=0;
            int totalDisqualifications=0;
            int totalNumGamesPlayed=0;
            boolean moreMaps = true;

            for(int m = 0; m < m_mapNames.length; ++m)
            {
                String mapName = m_mapNames[m];
                double avgWaypoints=0;
                double avgTimeSpent=0;
                int numGamesPlayed = 0;

                System.out.println("--------");
                System.out.println("Running " + m_controllerName + " in map " + mapName + "...");

                //For each trial...
                for(int i=0;i<trials;i++)
                {
                    System.out.println("Running trial " + i + "...");
                    // ... create a new game.
                    if(!prepareGame(m_mapNames[m], NUM_WAYPOINTS, AbstractController.rng))
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
                    System.out.print(i+"\t");
                    m_game.printResults();

                    // Save stats to csv file
                    writer.append(i + 1);
                    writer.append(mapName);
                    writer.append(m_game.getWaypointsVisited());
                    writer.append(m_game.getTotalTime());
                    writer.append(fmCallsSum / m_game.getTotalTime());
                    writer.append(msPerActionSum / m_game.getTotalTime());

                    // Compute score
                    double score = m_game.getWaypointsVisited() - m_game.getTotalTime() / 1000.;
                    scorePerTrial.add(score);
                }

                avgTotalWaypoints += (avgWaypoints / numGamesPlayed);
                avgTotalTimeSpent += (avgTimeSpent / numGamesPlayed);
                totalDisqualifications += (trials - numGamesPlayed);
                totalNumGamesPlayed += numGamesPlayed;

                //Print the average score.
                System.out.println("--------");
                System.out.format("Average waypoints: %.3f, average time spent: %.3f\n", (avgWaypoints / numGamesPlayed), (avgTimeSpent / numGamesPlayed));
                System.out.println("Disqualifications: " + (trials - numGamesPlayed) + "/" + trials);
            }

            //Print the average score.
            System.out.println("-------- Final score --------");
            System.out.format("Average waypoints: %.3f, average time spent: %.3f\n", (avgTotalWaypoints / m_mapNames.length), (avgTotalTimeSpent / m_mapNames.length));
            System.out.println("Disqualifications: " + (trials*m_mapNames.length - totalNumGamesPlayed) + "/" + trials*m_mapNames.length);
        }

        double scoreSum = 0;
        for(double score : scorePerTrial)
        {
            scoreSum += score;
        }
        return scoreSum / scorePerTrial.size();
    }

    public static int main(String[] args) throws Exception
    {
        if(args.length != 2)
        {
            System.err.println("Expected 2 arguments");
            return -1;
        }
        String algorithm = args[0];
        int numWaypoints = Integer.parseInt(args[1]);
        setup(algorithm, numWaypoints);

        System.out.println("Running experiment for algorithm " + ALGORITHM + " with " + NUM_WAYPOINTS + " waypoints");
        System.out.println("Score achieved: " + runExperiments(NUM_TRIALS, OutputDir));
        return 0;
    }

    private static void setup(String algorithm, int numWaypoints) throws Exception
    {
        ALGORITHM = algorithm;
        NUM_WAYPOINTS = numWaypoints;
        m_mapNames = new String[]{"maps/StageA/ptsp_map01.map", "maps/StageA/ptsp_map02.map",
                "maps/StageA/ptsp_map08.map", "maps/StageA/ptsp_map19.map",
                "maps/StageA/ptsp_map24.map", "maps/StageA/ptsp_map35.map",
                "maps/StageA/ptsp_map40.map", "maps/StageA/ptsp_map45.map",
                "maps/StageA/ptsp_map56.map", "maps/StageA/ptsp_map61.map"};

        // Algorithm
        if(algorithm == "QD-MCTS")
        {
            m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController";
            AutoSubgoalController.subgoalSearch = new MCTSNoveltySearch(4, new PositionBehaviourFunction(), AbstractController.rng);
            //AutoSubgoalController.explorationRate = highExplorationRate;
            //AutoSubgoalController.maxRolloutDepth = rolloutDepth;
            //MCTSNoveltySearch.explorationRate = lowExplorationRate;
            //MCTSNoveltySearch.maxSteps = steps;
        }
        else if(algorithm == "S-MCTS")
        {
            m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController";
            //PositionGridPredicate predicate = new PositionGridPredicate(cellSize, 3);
            RandomPredicateSearch.treatHorizonStatesAsSubgoals = true; // Now it behaves like S-MCTS
            //AutoSubgoalController.subgoalSearch = new RandomPredicateSearch(predicate, 4, steps, AbstractController.rng);
            //AutoSubgoalController.explorationRate = explorationRate;
            //AutoSubgoalController.maxRolloutDepth = rolloutDepth;
        }
        else if(algorithm == "MS-MCTS")
        {
            m_controllerName = "controllers.autoSubgoalMCTS.AutoSubgoalController";
            //PositionGridPredicate predicate = new PositionGridPredicate(cellSize, 3);
            RandomPredicateSearch.treatHorizonStatesAsSubgoals = false; // Now it behaves like MS-MCTS
            //AutoSubgoalController.subgoalSearch = new RandomPredicateSearch(predicate, 4, steps, AbstractController.rng);
            //AutoSubgoalController.explorationRate = explorationRate;
            //AutoSubgoalController.maxRolloutDepth = rolloutDepth;
        }
        else if(algorithm == "VanillaMCTS")
        {
            m_controllerName = "controllers.autoSubgoalMCTS.VanillaMCTS";
            //VanillaMCTS.explorationRate = explorationRate;
            //VanillaMCTS.maxRolloutDepth = rolloutDepth;
        }
        else if(algorithm == "VanillaGA")
        {
            m_controllerName = "controllers.autoSubgoalMCTS.GeneticAlgorithm.GAController";
            //GAController.GenomeLength = genomeLength;
            //GAController.PopulationSize = populationSize;
            //GAController.MutationRate = mutationRate;
        }
        else if(algorithm == "RB-MCTS")
        {
            m_controllerName = "controllers.mcts.MCTSController";
        }
        else
        {
            throw new Exception("Unexpected algorithm " + algorithm);
        }

        // Each controller tested inherits from the AbstractController, so with this we make sure that they all use the same parameters
        AbstractController.stopCondition = AbstractController.StopCondition.ForwardCalls;
        AbstractController.maxForwardCalls = 50000;
        AbstractController.rng.setSeed(0); // Ensure deterministic results, only deterministic if ForwardCalls are used as stop condition
    }

    public static String ALGORITHM = "";
    public static int NUM_WAYPOINTS = 10;
    public static int NUM_TRIALS = 10;
    public static String OutputDir = "./out";
}
