package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.SubgoalSearch.ISubgoalSearch;

import java.util.ArrayList;

public class SubgoalData
{
    public SubgoalData()
    {
        macroAction = new ArrayList<>();
    }

    public double[] latentState;
    public ArrayList<BaseAction> macroAction;
    public ISubgoalSearch subgoalSearch;
};
