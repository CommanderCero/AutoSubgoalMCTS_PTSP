package controllers.autoSubgoalMCTS;

import framework.core.Controller;
import framework.core.Game;

import java.util.Random;

public class AutoSubgoalController extends Controller
{
    public class PositionDistanceMetric implements IDistanceMetric
    {
        @Override
        public double computeDistance(Game state1, Game state2)
        {
            return state1.getShip().s.dist(state2.getShip().s);
        }
    }

    private AutoSubgoalMCTS algorithm;

    public AutoSubgoalController(Game game, long dueTimeMs)
    {
        algorithm = new AutoSubgoalMCTS(game, new PositionDistanceMetric(), 4, 4);
    }

    MCTSNode<AutoSubgoalMCTS.SubgoalData> currentChild;
    int index;
    Random rand = new Random();

    @Override
    public int getAction(Game game, long dueTimeMs)
    {
        algorithm = new AutoSubgoalMCTS(game, new PositionDistanceMetric(), 4, 4);
        long startTime = System.nanoTime();
        long timeBudgetMs = dueTimeMs - System.currentTimeMillis();
        if(currentChild != null && index >= currentChild.data.macroAction.size())
        {
            index = 0;
            algorithm.replaceRoot(currentChild, game);
            currentChild = null;
        }

        int counter = 0;
        if(currentChild == null)
        {
            // Run until the timeBudget is nearly used up, with a little bit of remaining time to collect the action
            while((System.nanoTime() - startTime) / 1000000 < timeBudgetMs - 2)
            {
                algorithm.step();
                counter++;
            }
            currentChild = algorithm.getBestChild();
        }

        if(currentChild == null)
        {
            System.out.println(counter);
            System.out.println("Error");
            return 0;
        }
        int nextAction = currentChild.data.macroAction.get(index);
        index++;
        return nextAction;
    }
}
