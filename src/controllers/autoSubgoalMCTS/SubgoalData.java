package controllers.autoSubgoalMCTS;

import java.util.ArrayList;

public class SubgoalData
{
    public SubgoalData()
    {
        macroAction = new ArrayList<>();
    }

    double[] latentState;
    ArrayList<BaseAction> macroAction;
    int stepCount = 0;
    SubgoalSearchMCTS subgoalSearch;
};
