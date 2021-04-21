package controllers.autoSubgoalMCTS;

import framework.core.Controller;
import framework.core.Game;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;

public class AutoSubgoalController extends Controller
{
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

    private AutoSubgoalMCTS algorithm;

    ArrayList<Game> states;
    ArrayList<Integer> actions;
    public AutoSubgoalController(Game game, long dueTimeMs)
    {
        states = new ArrayList<>();
        actions = new ArrayList<>();

        algorithm = new AutoSubgoalMCTS(game, new PositionBehaviourFunction(), 4, 300);
    }

    Random rand = new Random();
    int execCounter = 0;



    @Override
    public int getAction(Game game, long dueTimeMs)
    {
        algorithm.setInitialState(game.getCopy());

        long startTime = System.nanoTime();
        long timeBudgetMs = dueTimeMs - System.currentTimeMillis();
        int counter = 0;
        // Run until the timeBudget is nearly used up, with a little bit of remaining time to collect the action
        while((System.nanoTime() - startTime) / 1000000 < timeBudgetMs - 5)
        {
            algorithm.step();
            counter++;
        }

        System.out.println("Steps: " + counter);
        return algorithm.getNextAction();
    }

    @Override
    public void paint(Graphics2D graphics)
    {
        graphics.setColor(Color.yellow);
        drawSubgoals(graphics, algorithm.getRoot());
    }

    private void drawSubgoals(Graphics2D graphics, MCTSNode<AutoSubgoalMCTS.SubgoalData> node)
    {
        int r = 4;
        graphics.fillOval((int)node.data.latentState[0] - r, (int)node.data.latentState[1] - r, 2 * r, 2 * r);
        for(MCTSNode<AutoSubgoalMCTS.SubgoalData> child : node.children)
        {
            graphics.drawLine((int)node.data.latentState[0], (int)node.data.latentState[1], (int)child.data.latentState[0], (int)child.data.latentState[1]);
            drawSubgoals(graphics, child);
        }
    }
}
