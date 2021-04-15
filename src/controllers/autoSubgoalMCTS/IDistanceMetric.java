package controllers.autoSubgoalMCTS;

import framework.core.Game;

public interface IDistanceMetric
{
    double computeDistance(Game state1, Game state2);
}
