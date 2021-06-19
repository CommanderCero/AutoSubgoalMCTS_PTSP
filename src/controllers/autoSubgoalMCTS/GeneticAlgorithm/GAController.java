package controllers.autoSubgoalMCTS.GeneticAlgorithm;

import controllers.autoSubgoalMCTS.AbstractController;
import controllers.autoSubgoalMCTS.BaseAction;
import controllers.autoSubgoalMCTS.GeneticAlgorithm.Genome;
import controllers.autoSubgoalMCTS.RewardAccumulator;
import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Controller;
import framework.core.Game;
import framework.utils.Vector2d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

public class GAController extends AbstractController
{
    public static int GenomeLength = 20;
    public static int PopulationSize = 50;
    public static double MutationRate = 1. / GenomeLength;

    ArrayList<Genome<SearchData>> currPopulation;
    ArrayList<Genome<SearchData>> nextPopulation;
    RewardAccumulator rewardAccumulator;

    public GAController(Game game, long dueTimeMs)
    {
        currPopulation = new ArrayList<>(PopulationSize);
        nextPopulation = new ArrayList<>(PopulationSize);
        rewardAccumulator = new RewardAccumulator(0.99);

        for(int i = 0; i < PopulationSize; i++)
        {
            currPopulation.add(new Genome(GenomeLength, rng, new SearchData(GenomeLength + 1)));
            nextPopulation.add(new Genome(GenomeLength, rng, new SearchData(GenomeLength + 1)));
        }

        // ToDo Do not hardcode NaiveRewardGame here
        evaluateGenomes(currPopulation, new NaiveRewardGame(game));
    }

    @Override
    protected void step(RewardGame game)
    {
        // Keep a percentage of elite genomes
        int eliteCount = (int)(currPopulation.size() * 0.2);
        if(eliteCount % 2 == 1)
            eliteCount -= 1; // Make sure it's a multiple of 2 to not mess up the crossover part
        currPopulation.sort(Comparator.comparingDouble((g) -> -g.score));
        for(int i = 0; i < eliteCount; i++)
        {
            nextPopulation.get(i).copyOver(currPopulation.get(i));
        }

        // Compute smallest value for normalizing negative fitness
        double minValue = Double.POSITIVE_INFINITY;
        for(Genome g : currPopulation)
        {
            minValue = Math.min(minValue, g.score);
        }

        double scoreSum = 0;
        for(Genome g : currPopulation)
        {
            g.score = (g.score - minValue) + 1; // Add +1 to make sure the smallest value also has a chance to be selected
            scoreSum += g.score;
        }

        // Roulette Wheel Crossover
        assert((nextPopulation.size() - eliteCount) % 2 == 0);
        for(int i = eliteCount; i < nextPopulation.size(); i += 2)
        {
            // Select parents
            double p1Prob = rng.nextDouble();
            double p2Prob = rng.nextDouble();
            int p1Index = -1;
            int p2Index = -1;
            double sum = 0;
            for(int x = 0; x < currPopulation.size() && (p1Index == -1 || p2Index == -1); x++)
            {
                sum += currPopulation.get(x).score / scoreSum;
                if (p1Index == -1 && p1Prob <= sum)
                {
                    p1Index = x;
                }
                if (p2Index == -1 && p2Prob <= sum)
                {
                    p2Index = x;
                }
            }

            Genome p1 = nextPopulation.get(p1Index);
            Genome p2 = nextPopulation.get(p2Index);
            p1.copyOver(currPopulation.get(p1Index));
            p2.copyOver(currPopulation.get(p2Index));
            p1.crossover(p2, rng);
        }

        // Mutation
        for(int i = eliteCount; i < nextPopulation.size(); i++)
        {
            nextPopulation.get(i).mutate(MutationRate, rng);
        }

        // Update scores
        evaluateGenomes(currPopulation, game);
    }

    @Override
    protected BaseAction getBestAction()
    {
        // Select a new action
        Genome bestGenome = currPopulation.get(0);
        for(int i = 1; i < currPopulation.size(); i++)
        {
            if(bestGenome.score < currPopulation.get(i).score)
            {
                bestGenome = currPopulation.get(i);
            }
        }
        BaseAction nextAction = new BaseAction(bestGenome.actions[0]);

        // Update population
        for(Genome g : currPopulation)
        {
            // Remove the first action and sample a random one at the end
            for(int i = 0; i < g.actions.length - 1; i++)
            {
                g.actions[i] = g.actions[i + 1];
            }
            g.actions[g.actions.length - 1] = rng.nextInt(NUM_ACTIONS);
        }

        return nextAction;
    }

    @Override
    public synchronized void paint(Graphics2D graphics)
    {
        for(int x = 0; x < currPopulation.size(); x++)
        {
            Genome<SearchData> g = currPopulation.get(x);
            for(int i = 0; i < g.data.trajectory.length - 1; i++)
            {
                graphics.drawLine((int)g.data.trajectory[i].x, (int)g.data.trajectory[i].y, (int)g.data.trajectory[i + 1].x, (int)g.data.trajectory[i + 1].y);
            }
        }
    }

    private void evaluateGenomes(ArrayList<Genome<SearchData>> genomes, RewardGame game)
    {
        for(int i = 0; i < genomes.size(); i++)
        {
            Genome<SearchData> currGenome = genomes.get(i);
            RewardGame copy = game.getCopy();

            rewardAccumulator.reset();
            BaseAction baseAction = new BaseAction(-1);
            int x = 0;
            for(; x < currGenome.actions.length && !copy.isEnded(); x++)
            {
                currGenome.data.trajectory[x] = copy.getState().getShip().s.copy();
                baseAction.lowLevelAction = currGenome.actions[x];
                rewardAccumulator.addReward(baseAction.apply(copy));
            }
            currGenome.data.trajectory[x] = copy.getState().getShip().s.copy();
            currGenome.score = rewardAccumulator.getRewardSum();
        }
    }
}
