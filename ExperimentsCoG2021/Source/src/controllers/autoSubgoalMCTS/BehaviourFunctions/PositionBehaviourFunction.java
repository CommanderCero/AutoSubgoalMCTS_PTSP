package controllers.autoSubgoalMCTS.BehaviourFunctions;

import framework.core.Game;

public class PositionBehaviourFunction implements IBehaviourFunction
{
    @Override
    public void toLatent(Game state, double[] bucket)
    {
        bucket[0] = state.getShip().s.x;
        bucket[1] = state.getShip().s.y;
    }

    @Override
    public int getLatentSize()
    {
        return 2;
    }
}
