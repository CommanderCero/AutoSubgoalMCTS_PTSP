package controllers.autoSubgoalMCTS;

import framework.core.Controller;
import framework.core.Game;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
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

    public AutoSubgoalController(Game game, long dueTimeMs)
    {
        algorithm = new AutoSubgoalMCTS(game, new PositionBehaviourFunction(), 4, 300);
    }

    MCTSNode<AutoSubgoalMCTS.SubgoalData> lastSelectedNode;
    int actionIndex;
    Random rand = new Random();
    int execCounter = 0;

    @Override
    public int getAction(Game game, long dueTimeMs)
    {
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
        if(lastSelectedNode == null || actionIndex == lastSelectedNode.data.macroAction.size())
        {
            lastSelectedNode = algorithm.getBestChild();
            actionIndex = 0;
            execCounter = 0;

            if(lastSelectedNode == null)
            {
                System.out.println("Error");
                algorithm = new AutoSubgoalMCTS(game, new PositionBehaviourFunction(), 4, 300);
                return 0;
            }

            // The game is deterministic, so we can update immediatly
            algorithm.advanceTree(lastSelectedNode);
        }

        int nextAction = lastSelectedNode.data.macroAction.get(actionIndex);
        execCounter++;
        if(execCounter == 10)
        {
            execCounter = 0;
            actionIndex++;
        }
        return nextAction;
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
        //graphics.fillOval((int)node.data.latentState[0] - r, (int)node.data.latentState[1] - r, 2 * r, 2 * r);
        for(MCTSNode<AutoSubgoalMCTS.SubgoalData> child : node.children)
        {
            graphics.drawLine((int)node.data.latentState[0], (int)node.data.latentState[1], (int)child.data.latentState[0], (int)child.data.latentState[1]);
            drawSubgoals(graphics, child);
        }
    }
}
