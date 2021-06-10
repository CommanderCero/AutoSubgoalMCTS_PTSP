package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;
import framework.utils.Vector2d;

import java.util.ArrayList;

public class SubgoalData
{
    public SubgoalData()
    {
        macroAction = new MacroAction();
    }

    public Vector2d lastSeenPosition;
    public double[] latentState;
    public double noveltyScore;
    public double noveltyLowerBound;
    public double noveltyUpperBound;
    public MacroAction macroAction;
    public ISubgoalSearch subgoalSearch;
};
