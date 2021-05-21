package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import framework.core.Game;

import java.util.ArrayList;

public class MacroAction
{
    public MacroAction() {actions = new ArrayList<>();}

    public void apply(RewardGame state)
    {
        for(BaseAction a : actions)
        {
            a.apply(state);
            if(state.isEnded())
                return;
        }
    }

    public int size() {return actions.size();}

    public ArrayList<BaseAction> actions;
}
