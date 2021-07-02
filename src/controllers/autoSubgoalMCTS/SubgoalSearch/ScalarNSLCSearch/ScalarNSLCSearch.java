package controllers.autoSubgoalMCTS.SubgoalSearch.ScalarNSLCSearch;

import controllers.autoSubgoalMCTS.*;
import controllers.autoSubgoalMCTS.BehaviourFunctions.IBehaviourFunction;
import controllers.autoSubgoalMCTS.GeneticAlgorithm.Genome;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.ScalarNSLCSearch.SearchData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class ScalarNSLCSearch implements ISubgoalSearch
{
    public static int GenomeLength = 4;
    public static int PopulationSize = 20;
    public static double MutationRate = 1. / GenomeLength;

    ArrayList<Genome<SearchData>> currPopulation;
    ArrayList<Genome<SearchData>> nextPopulation;
    RewardAccumulator rewardAccumulator;

    ArrayList<SearchData> subgoalArchive;

    IBehaviourFunction behaviourFunction;
    Random rng;
    int maxSteps;
    int steps;
    int maxStagnationCount;
    int stagnationCount;
    double bestFoundScore;

    public ScalarNSLCSearch(IBehaviourFunction behaviourFunction, Random rng, int steps, int stagnationCount)
    {
        this.behaviourFunction = behaviourFunction;
        this.rng = rng;
        this.maxSteps = steps;
        this.steps = 0;
        this.maxStagnationCount = stagnationCount;
        this.stagnationCount = 0;
        this.bestFoundScore = Double.NEGATIVE_INFINITY;
        currPopulation = new ArrayList<>(PopulationSize);
        nextPopulation = new ArrayList<>(PopulationSize);
        subgoalArchive = new ArrayList<>();
        rewardAccumulator = new RewardAccumulator(0.99);

        for(int i = 0; i < PopulationSize; i++)
        {
            currPopulation.add(new Genome(GenomeLength, rng, new SearchData()));
            nextPopulation.add(new Genome(GenomeLength, rng, new SearchData()));
        }
    }

    @Override
    public int step(RewardGame state)
    {
        steps = 0;
        stagnationCount = 0;
        bestFoundScore = Double.NEGATIVE_INFINITY;
        currPopulation = new ArrayList<>(PopulationSize);
        nextPopulation = new ArrayList<>(PopulationSize);
        for(int i = 0; i < PopulationSize; i++)
        {
            currPopulation.add(new Genome(GenomeLength, rng, new SearchData()));
            nextPopulation.add(new Genome(GenomeLength, rng, new SearchData()));
        }

        Genome<SearchData> bestGenome = null;
        while(true)
        {
            steps++;
            stagnationCount++;

            // Update scores
            runGenomes(currPopulation, state);
            bestGenome = evaluateGenomes(currPopulation);
            if(bestGenome.score > bestFoundScore)
            {
                stagnationCount = 0;
                bestFoundScore = bestGenome.score;
            }
            else if(stagnationCount == maxStagnationCount)
            {
                break;
            }

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
                nextPopulation.get(i).mutate(MutationRate, rng);
            }
        }

        System.out.println("Converged after " + steps + " steps");

        subgoalArchive.add(bestGenome.data);
        // Apply best genome to the state passed to us, such that the corresponding state can be used by the high-level search
        for(int x = 0; x < bestGenome.actions.length && !state.isEnded(); x++)
        {
            BaseAction baseAction = new BaseAction(bestGenome.actions[x]);
            baseAction.apply(state);
        }

        return GenomeLength;
    }

    @Override
    public boolean isDone()
    {
        return subgoalArchive.size() > 5;
    }

    @Override
    public ArrayList<MacroAction> getMacroActions()
    {
        ArrayList<MacroAction> actions = new ArrayList<>();
        for(SearchData subgoal : subgoalArchive)
        {
            actions.add(subgoal.macroAction);
        }
        return actions;
    }

    @Override
    public ISubgoalSearch createNewSearch(MCTSNode<SubgoalData> parentNode)
    {
        return new ScalarNSLCSearch(behaviourFunction, rng, maxSteps, maxStagnationCount);
    }

    private void runGenomes(ArrayList<Genome<SearchData>> genomes, RewardGame game)
    {

        for(int i = 0; i < genomes.size(); i++)
        {
            Genome<SearchData> currGenome = genomes.get(i);
            RewardGame copy = game.getCopy();

            rewardAccumulator.reset();
            // Apply actions
            MacroAction macroAction = new MacroAction();
            for(int x = 0; x < currGenome.actions.length && !copy.isEnded(); x++)
            {
                BaseAction baseAction = new BaseAction(currGenome.actions[x]);
                rewardAccumulator.addReward(baseAction.apply(copy));
                macroAction.actions.add(baseAction);
            }

            // Update data in the genome
            currGenome.data.reward = rewardAccumulator.getRewardSum();
            currGenome.data.latentState = new double[behaviourFunction.getLatentSize()];
            behaviourFunction.toLatent(copy.getState(), currGenome.data.latentState);
            currGenome.data.macroAction = macroAction;
        }
    }

    private Genome<SearchData> evaluateGenomes(ArrayList<Genome<SearchData>> genomes)
    {
        if(subgoalArchive.size() == 0)
        {
            return evaluateGenomesRewardOnly(genomes);
        }

        return evaluateGenomesNoveltyAndReward(genomes);
    }

    private Genome evaluateGenomesRewardOnly(ArrayList<Genome<SearchData>> genomes)
    {
        Genome<SearchData> bestGenome = genomes.get(0);
        for(Genome<SearchData> g : genomes)
        {
            g.score = g.data.reward;
            if(g.score > bestGenome.score)
            {
                bestGenome = g;
            }
        }

        return bestGenome;
    }

    private Genome evaluateGenomesNoveltyAndReward(ArrayList<Genome<SearchData>> genomes)
    {
        double[] noveltyCache = new double[genomes.size()];
        double[] rewardCache = new double[genomes.size()];
        double minNovelty = Double.POSITIVE_INFINITY;
        double maxNovelty = Double.NEGATIVE_INFINITY;

        // Compute the novelty & reward for each candidate
        for(int i = 0; i < genomes.size(); i++)
        {
            Genome<SearchData> genome = genomes.get(i);

            // Find the n-closest neighbors from all subgoals
            int n = subgoalArchive.size() > 3 ? 3 : subgoalArchive.size();
            Collections.sort(subgoalArchive, (SearchData v1, SearchData v2) ->
            {
                double v1Dist = latentDist(v1.latentState, genome.data.latentState);
                double v2Dist = latentDist(v2.latentState, genome.data.latentState);
                return v1Dist > v2Dist ? 1 : v1Dist < v2Dist ? -1 : 0;
            });

            // Compute novelty & rewards scores
            noveltyCache[i] = 0;
            rewardCache[i] = 3;
            for(int x = 0; x < n; x++)
            {
                noveltyCache[i] += latentDist(genome.data.latentState, subgoalArchive.get(x).latentState);
                if(subgoalArchive.get(x).reward >= genome.data.reward)
                {
                    rewardCache[i]--;
                }
            }
            minNovelty = Math.min(minNovelty, noveltyCache[i]);
            maxNovelty = Math.max(maxNovelty, noveltyCache[i]);
        }

        // Compute scores
        Genome bestGenome = genomes.get(0);
        for(int i = 0; i < genomes.size(); i++)
        {
            Genome<SearchData> g = genomes.get(i);
            g.score = (noveltyCache[i] - minNovelty) / (maxNovelty - minNovelty + 1e-8);
            g.score = 0.5 * g.score + 0.5 * (rewardCache[i] / 3);

            if(g.score > bestGenome.score)
            {
                bestGenome = g;
            }
        }
        return bestGenome;
    }

    private double latentDist(double[] v1, double[] v2)
    {
        double sumSquared = 0;
        for(int i = 0; i < v1.length; i++)
        {
            double delta = v1[i] - v2[i];
            sumSquared += delta * delta;
        }
        return Math.sqrt(sumSquared);
    }
}
