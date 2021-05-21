package controllers.autoSubgoalMCTS;

import framework.core.Game;

import java.util.ArrayList;

public class MacroAction
{
    public MacroAction() {actions = new ArrayList<>();}

    public void apply(Game state)
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
