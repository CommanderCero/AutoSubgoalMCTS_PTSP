package controllers.autoSubgoalMCTS;

import framework.core.Game;

public interface IBehaviourFunction
{
    void toLatent(Game state, double[] bucket);
    int getLatentSize();
}
