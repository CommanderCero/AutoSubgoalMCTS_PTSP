package controllers.autoSubgoalMCTS.SubgoalSearch;

import controllers.autoSubgoalMCTS.MCTSNode;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalData;

public interface ISubgoalSearch
{
    void step(RewardGame state);
    boolean isDone();
    void addSubgoals(MCTSNode<SubgoalData> parentNode);
    ISubgoalSearch copy();
}
