package controllers.autoSubgoalMCTS.SubgoalPredicates;

import framework.core.Game;
import java.awt.*;

public interface ISubgoalPredicate
{
    boolean isSubgoal(Game state);
    boolean isSameSubgoal(Game state1, Game state2);
    boolean isSameState(Game state1, Game state2);
    void render(Graphics2D graphics, Game state);
}
