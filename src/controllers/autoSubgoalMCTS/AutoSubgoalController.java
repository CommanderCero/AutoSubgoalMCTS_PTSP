package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.MCTSNoveltySearch;
import framework.core.Controller;
import framework.core.Game;
import framework.utils.Vector2d;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class AutoSubgoalController extends Controller
{
    public class PositionBehaviourFunction implements IBehaviourFunction
    {
        @Override
        public void toLatent(Game state, double[] bucket)
        {
            bucket[0] = state.getShip().s.x;
            bucket[1] = state.getShip().s.y;
            // v = new Vector2d(state.getShip().v);
            //v.normalise();
            //bucket[2] = v.x;
            //bucket[3] = v.y;
        }

        @Override
        public int getLatentSize()
        {
            return 2;
        }
    }

    private AutoSubgoalMCTS algorithm;

    ArrayList<Game> states;
    ArrayList<Integer> actions;
    public AutoSubgoalController(Game game, long dueTimeMs)
    {
        states = new ArrayList<>();
        actions = new ArrayList<>();

        RewardGame rGame = new NaiveRewardGame(game);
        algorithm = new AutoSubgoalMCTS(rGame, new MCTSNoveltySearch(4, new PositionBehaviourFunction()), 300);
    }

    @Override
    public synchronized int getAction(Game game, long dueTimeMs)
    {
        algorithm.setInitialState(game.getCopy());

        long startTime = System.nanoTime();
        long timeBudgetMs = dueTimeMs - System.currentTimeMillis();
        int counter = 0;
        // Run until the timeBudget is used up, with a little bit of remaining time to collect the action
        while((System.nanoTime() - startTime) / 1000000 < timeBudgetMs - 5)
        {
            algorithm.step();
            counter++;
        }

        int action = algorithm.getNextAction();
        //System.out.println("Action: " + action + "\tSteps: " + counter);
        return action;
    }

    @Override
    public synchronized void paint(Graphics2D graphics)
    {
        graphics.setColor(Color.yellow);
        drawSubgoals(graphics, algorithm.getRoot());
    }

    private void drawSubgoals(Graphics2D graphics, MCTSNode<SubgoalData> node)
    {
        int r = 2;
        if(node.data.latentState != null)
        {
            graphics.fillOval((int)node.data.latentState[0] - r, (int)node.data.latentState[1] - r, 2 * r, 2 * r);
        }

        for(int i = 0; i < node.children.size(); i++)
        {
            MCTSNode<SubgoalData> child = node.children.get(i);
            if(node.data.latentState != null) {
                graphics.drawLine((int) node.data.latentState[0], (int) node.data.latentState[1], (int) child.data.latentState[0], (int) child.data.latentState[1]);
            }
            drawSubgoals(graphics, child);
        }
    }
}
