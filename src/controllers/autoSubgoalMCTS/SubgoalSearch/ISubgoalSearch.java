package controllers.autoSubgoalMCTS.SubgoalSearch;

import controllers.autoSubgoalMCTS.MCTSNode;
import controllers.autoSubgoalMCTS.MacroAction;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalData;
import framework.core.Game;

import java.awt.*;
import java.util.ArrayList;

public interface ISubgoalSearch
{
    void step(RewardGame state);
    boolean isDone();
    ArrayList<MacroAction> getMacroActions();
    ISubgoalSearch copy();
}
