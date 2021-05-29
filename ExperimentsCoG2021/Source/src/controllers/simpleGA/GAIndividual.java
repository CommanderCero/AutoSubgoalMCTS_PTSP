package controllers.simpleGA;

import controllers.mcts.MacroAction;
import controllers.heuristic.GameEvaluator;
import framework.core.Game;

import java.util.Random;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 17/10/12
 */
public class GAIndividual
{
    public int[] m_genome;
    public double m_fitness;
    public final double MUTATION_PROB = 0.2; //0.834=5/6   //0.2;

    public GAIndividual(int a_genomeLength)
    {
        m_genome = new int[a_genomeLength];
        m_fitness = 0;
    }

    public void randomize(Random a_rnd, int a_numActions)
    {
        for(int i = 0; i < m_genome.length; ++i)
        {
            m_genome[i] = /*3+*/ a_rnd.nextInt(a_numActions);
        }
    }

    public Game evaluate(Game a_gameState, GameEvaluator a_gameEvaluator)
    {
        Game thisGameCopy = a_gameState.getCopy();
        boolean end = false;
        for(int i = 0; i < m_genome.length; ++i)
        {
            int thisAction = m_genome[i];
            for(int j =0; !end && j < GameEvaluator.MACRO_ACTION_LENGTH; ++j)
            {
                thisGameCopy.tick(thisAction);
                end = a_gameEvaluator.isEndGame(thisGameCopy);
            }
        }
        m_fitness = a_gameEvaluator.scoreGame(thisGameCopy);
        return thisGameCopy;
    }

    public void mutate(Random a_rnd)
    {
        for (int i = 0; i < m_genome.length; i++) {
            if(a_rnd.nextDouble() < MUTATION_PROB)
            {
                if(a_rnd.nextDouble() < 0.5)  //mutate thrust
                    m_genome[i] = MacroAction.mutateThrust(m_genome[i]);
                else  //mutate steering
                    m_genome[i] = MacroAction.mutateSteer(m_genome[i], a_rnd.nextDouble()>0.5);
            }

        }
    }

    /**
     * Returns a NEW INDIVIDUAL, crossed uniformly from this and the received parent.
     */
    public GAIndividual uniformCross(GAIndividual ind, Random a_rnd)
    {
        GAIndividual newInd = new GAIndividual(this.m_genome.length);

        for(int i = 0; i < this.m_genome.length; ++i)
        {
            if(a_rnd.nextFloat() < 0.5f)
            {
                newInd.m_genome[i] = this.m_genome[i];
            }else{
                newInd.m_genome[i] = ind.m_genome[i];
            }
        }

        return newInd;
    }


    public GAIndividual copy()
    {
        GAIndividual gai = new GAIndividual(this.m_genome.length);
        for(int i = 0; i < this.m_genome.length; ++i)
        {
            gai.m_genome[i] = this.m_genome[i];
        }
        return gai;
    }

    public String toString()
    {
        String st = new String();
        for(int i = 0; i < m_genome.length; ++i)
            st += m_genome[i];
        return st;
    }


}
