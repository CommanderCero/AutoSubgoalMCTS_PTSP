package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.heuristic.GameEvaluator;
import controllers.heuristic.TSPGraphPhysicsEst;
import controllers.heuristic.graph.Graph;
import controllers.mcts.TSPGraph;
import framework.core.Controller;
import framework.core.Game;
import controllers.autoSubgoalMCTS.MCTSNode;
import framework.utils.Vector2d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class MyMCTSController extends Controller
{
    private class MCTSData
    {
        public MCTSData(int action, Vector2d position)
        {
            this.action = new BaseAction(action);
            this.position = position;
        }

        BaseAction action;
        Vector2d position;
    }

    public Random rand = new Random(0);
    public double explorationRate = Math.sqrt(2);
    public int maxRolloutDepth = 8;

    private MCTSNode<MCTSData> root;
    private Graph graph;
    private TSPGraphPhysicsEst tspGraph;
    private GameEvaluator evaluator;

    public MyMCTSController(Game game, long dueTimeMs)
    {
        root = new MCTSNode<MCTSData>(new MCTSData(-1, game.getShip().s));
        // Initialize structures for finding the best ordering of waypoints
        graph = new Graph(game);
        tspGraph = new TSPGraphPhysicsEst(game, graph);
        tspGraph.solve();
        // Initialize evaluator to compute a good heuristic
        evaluator = new GameEvaluator(tspGraph, graph, true);
    }

    @Override
    public int getAction(Game a_game, long dueTimeMs)
    {
        RewardGame initialState = new NaiveRewardGame(a_game.getCopy());
        RewardAccumulator accumulator = new RewardAccumulator(0.99);

        long startTime = System.nanoTime();
        long timeBudgetMs = dueTimeMs - System.currentTimeMillis();
        int counter = 0;
        // Run until the timeBudget is used up, with a little bit of remaining time to collect the action
        while((System.nanoTime() - startTime) / 1000000 < timeBudgetMs - 5)
        {
            stepMCTS(initialState.getCopy(), accumulator);
            counter++;
        }

        MCTSNode<MCTSData> bestChild = root.getChildWithHighestReturn();
        // Reuse tree
        bestChild.data.action.repetitions--;
        if(bestChild.data.action.repetitions == 0)
        {
            root = bestChild;
            root.parent = null;
        }

        //System.out.println("Steps=" + counter + "\tTotal root visits=" + root.visitCount);
        return bestChild.data.action.lowLevelAction;
    }

    private void stepMCTS(RewardGame state, RewardAccumulator accumulator)
    {
        // Selection
        int depth = 0;
        MCTSNode<MCTSData> currNode = root;
        while (currNode.children.size() == NUM_ACTIONS)
        {
            currNode = currNode.selectUCT(explorationRate);
            currNode.data.action.apply(state, accumulator);
            depth++;
        }

        if (!state.isEnded())
        {
            // Expansion
            int nextAction = currNode.children.size();
            MCTSData newData = new MCTSData(nextAction, state.getState().getShip().s);
            currNode = currNode.addChild(newData);
            depth++;

            // Simulation
            rollout(state, depth, accumulator);
        }

        // Backpropagation
        currNode.backpropagate(accumulator.getRewardSum());
        accumulator.reset();
    }

    private void rollout(RewardGame state, int currentDepth, RewardAccumulator accumulator)
    {
        while(!state.isEnded() && currentDepth <= maxRolloutDepth)
        {
            BaseAction nextAction = new BaseAction(rand.nextInt(NUM_ACTIONS));
            nextAction.apply(state, accumulator);

            currentDepth++;
        }
    }

    @Override
    public synchronized void paint(Graphics2D graphics)
    {
        graphics.setColor(Color.yellow);
        drawTree(graphics, root);
    }

    private void drawTree(Graphics2D graphics, MCTSNode<MCTSData> node)
    {
        for(int i = 0; i < node.children.size(); i++)
        {
            MCTSNode<MCTSData> child = node.children.get(i);
            graphics.drawLine((int)node.data.position.x, (int)node.data.position.y, (int)child.data.position.x, (int)child.data.position.y);
            drawTree(graphics, child);
        }
    }
}
