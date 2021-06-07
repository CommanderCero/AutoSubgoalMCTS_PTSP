package controllers.autoSubgoalMCTS.GeneticAlgorithm;

import controllers.autoSubgoalMCTS.BaseAction;
import controllers.autoSubgoalMCTS.RewardAccumulator;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Controller;
import framework.utils.Vector2d;

import java.util.Random;

public class Genome<Data>
{
    public double score;
    public int[] actions;
    public Data data;

    public Genome(int size, Random rng, Data data)
    {
        actions = new int[size];
        this.data = data;
        randomize(rng);
    }

    public void randomize(Random rng)
    {
        for(int i = 0; i < actions.length; i++)
        {
            actions[i] = rng.nextInt(Controller.NUM_ACTIONS);
        }
    }

    public void crossover(Genome other, Random rng)
    {
        int crossoverPoint = rng.nextInt(actions.length - 1);
        for (int i = 0; i <= crossoverPoint; i++)
        {
            int tmp = actions[i];
            actions[i] = other.actions[i];
            other.actions[i] = tmp;
        }
    }

    public void mutate(Random rng)
    {
        double mutationRate = 0.01;
        for (int i = 0; i < actions.length; i++)
        {
            if (rng.nextDouble() <= mutationRate)
            {
                actions[i] = rng.nextInt(Controller.NUM_ACTIONS);
            }
        }
    }

    public void copyOver(Genome<Data> reference)
    {
        score = reference.score;
        for(int i = 0; i < actions.length; i++)
        {
            actions[i] = reference.actions[i];
            data = reference.data;
        }
    }
}
