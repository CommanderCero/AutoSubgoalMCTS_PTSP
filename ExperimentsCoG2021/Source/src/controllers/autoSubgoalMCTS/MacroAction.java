package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Game;

import java.util.ArrayList;

public class MacroAction
{
    public MacroAction() {actions = new ArrayList<>();}

    public double apply(RewardGame state)
    {
        double rewardSum = 0;
        for(BaseAction a : actions)
        {
            rewardSum += a.apply(state);
            if(state.isEnded())
            {
                break;
            }
        }
        return rewardSum;
    }

    public int size() {return actions.size();}

    public ArrayList<BaseAction> actions;
}
