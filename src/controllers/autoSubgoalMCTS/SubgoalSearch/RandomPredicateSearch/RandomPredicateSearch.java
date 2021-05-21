package controllers.autoSubgoalMCTS.SubgoalSearch.RandomPredicateSearch;

import controllers.autoSubgoalMCTS.BaseAction;
import controllers.autoSubgoalMCTS.MacroAction;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalPredicates.ISubgoalPredicate;
import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import framework.core.Game;

import java.util.ArrayList;
import java.util.Random;

public class RandomPredicateSearch implements ISubgoalSearch
{
    public RandomPredicateSearch(ISubgoalPredicate predicate, int horizon, int steps, Random rng)
    {
        this.predicate = predicate;
        this.macroActions = new ArrayList<>();
        this.states = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.horizon = horizon;
        this.steps = steps;
        this.rng = rng;

        this.stepCount = 0;
    }

    @Override
    public void step(RewardGame state)
    {
        stepCount++;

        // Sample a random macro action
        MacroAction macroAction = new MacroAction();
        double rewardBefore = state.getRewardSum();
        for(int i = 0; i < horizon && !state.isEnded(); i++)
        {
            BaseAction action = new BaseAction();
            action.sample(rng);
            macroAction.actions.add(action);

            action.apply(state);
            if(predicate.isSubgoal(state.getState()))
            {
                break;
            }
        }
        double rewardAfter = state.getRewardSum();

        // Update macro actions
        if(predicate.isSubgoal(state.getState()))
        {
            boolean found = false;
            for(int i = 0; i < macroActions.size(); i++)
            {
                if(predicate.isSameSubgoal(state.getState(), states.get(i)))
                {
                    // Check if we found a better trajectory
                    if(rewards.get(i) < (rewardAfter - rewardBefore))
                    {
                        // Replace macro action
                        macroActions.set(i, macroAction);
                    }

                    found = true;
                    break;
                }
            }

            // We found a new subgoal
            if(!found)
            {
                states.add(state.getState().getCopy());
                rewards.add(rewardAfter - rewardBefore);
                macroActions.add(macroAction);
            }
        }
    }

    @Override
    public boolean isDone()
    {
        if(stepCount >= steps && macroActions.size() == 0)
        {
            // We did not find any subgoals in time
            // Although this should not happen, we will increase the computation time for this node
            stepCount = 0;
        }

        return stepCount >= steps;
    }

    @Override
    public ArrayList<MacroAction> getMacroActions()
    {
        return macroActions;
    }

    @Override
    public ISubgoalSearch copy()
    {
        RandomPredicateSearch subgoalSearch = new RandomPredicateSearch(predicate, horizon, steps, rng);
        return subgoalSearch;
    }

    Random rng;
    private ISubgoalPredicate predicate;
    private ArrayList<MacroAction> macroActions;
    private ArrayList<Game> states;
    private ArrayList<Double> rewards;
    private int stepCount;
    private int steps;
    private int horizon;
}
