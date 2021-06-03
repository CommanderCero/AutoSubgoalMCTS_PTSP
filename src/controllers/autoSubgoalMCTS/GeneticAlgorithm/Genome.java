package controllers.autoSubgoalMCTS.GeneticAlgorithm;

import controllers.autoSubgoalMCTS.BaseAction;
import controllers.autoSubgoalMCTS.RewardAccumulator;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Controller;
import framework.utils.Vector2d;

import java.util.Random;

public class Genome
{
    public double score;
    public int[] actions;
    public Vector2d[] trajectory;

    public Genome(int size, Random rng)
    {
        actions = new int[size];
        trajectory = new Vector2d[size + 1];
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
        double rand = rng.nextDouble();
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

    public void apply(RewardGame game, RewardAccumulator accumulator)
    {
        BaseAction baseAction = new BaseAction(-1);
        int i = 0;
        for(; i < actions.length && !game.isEnded(); i++)
        {
            trajectory[i] = game.getState().getShip().s.copy();
            baseAction.lowLevelAction = actions[i];
            accumulator.addReward(baseAction.apply(game));
        }
        trajectory[i] = game.getState().getShip().s.copy();
    }

    public void copyOver(Genome reference)
    {
        score = reference.score;
        for(int i = 0; i < actions.length; i++)
        {
            actions[i] = reference.actions[i];
        }
    }
}
