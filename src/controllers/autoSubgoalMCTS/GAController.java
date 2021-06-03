package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.GeneticAlgorithm.Genome;
import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Controller;
import framework.core.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class GAController extends AbstractController
{
    public static int GenomeLength = 20;
    public static int PopulationSize = 50;

    ArrayList<Genome> currPopulation;
    ArrayList<Genome> nextPopulation;
    RewardAccumulator rewardAccumulator;

    public GAController(Game game, long dueTimeMs)
    {
        currPopulation = new ArrayList<>(PopulationSize);
        nextPopulation = new ArrayList<>(PopulationSize);
        rewardAccumulator = new RewardAccumulator(0.99);

        for(int i = 0; i < PopulationSize; i++)
        {
            currPopulation.add(new Genome(GenomeLength, rng));
            nextPopulation.add(new Genome(GenomeLength, rng));
        }

        // ToDo Do not hardcode NaiveRewardGame here
        evaluateGenomes(currPopulation, new NaiveRewardGame(game));
    }

    @Override
    protected void step(RewardGame game)
    {
        // Keep a percentage of elite genomes
        int eliteCount = (int)(currPopulation.size() * 0.2);
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
            nextPopulation.get(i).mutate(rng);
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
            Genome g = currPopulation.get(x);
            for(int i = 0; i < g.trajectory.length - 1; i++)
            {
                graphics.drawLine((int)g.trajectory[i].x, (int)g.trajectory[i].y, (int)g.trajectory[i + 1].x, (int)g.trajectory[i + 1].y);
            }
        }
    }

    private void evaluateGenomes(ArrayList<Genome> genomes, RewardGame game)
    {
        for(int i = 0; i < genomes.size(); i++)
        {
            Genome currGenome = genomes.get(i);
            RewardGame copy = game.getCopy();

            rewardAccumulator.reset();
            currGenome.apply(copy, rewardAccumulator);
            currGenome.score = rewardAccumulator.getRewardSum();
        }
    }
}
