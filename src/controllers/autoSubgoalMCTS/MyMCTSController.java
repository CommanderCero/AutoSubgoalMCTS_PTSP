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

public class MyMCTSController extends AbstractController
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

    public double explorationRate = Math.sqrt(2);
    public int maxRolloutDepth = 8;

    private MCTSNode<MCTSData> root;
    private Graph graph;
    private TSPGraphPhysicsEst tspGraph;
    private GameEvaluator evaluator;
    private RewardAccumulator accumulator;

    public MyMCTSController(Game game, long dueTimeMs)
    {
        root = new MCTSNode<MCTSData>(new MCTSData(-1, game.getShip().s));
        // Initialize structures for finding the best ordering of waypoints
        graph = new Graph(game);
        tspGraph = new TSPGraphPhysicsEst(game, graph);
        tspGraph.solve();
        // Initialize evaluator to compute a good heuristic
        evaluator = new GameEvaluator(tspGraph, graph, true);

        // Initialize reward accumulator
        accumulator = new RewardAccumulator(0.99);
    }

    @Override
    protected void step(RewardGame game)
    {
        // Selection
        int depth = 0;
        MCTSNode<MCTSData> currNode = root;
        while (currNode.children.size() == NUM_ACTIONS)
        {
            currNode = currNode.selectUCT(explorationRate);
            currNode.data.action.apply(game, accumulator);
            depth++;
        }

        if (!game.isEnded())
        {
            // Expansion
            int nextAction = currNode.children.size();
            MCTSData newData = new MCTSData(nextAction, game.getState().getShip().s);
            currNode = currNode.addChild(newData);
            depth++;

            // Simulation
            rollout(game, depth, accumulator);
        }

        // Backpropagation
        currNode.backpropagate(accumulator.getRewardSum());
        accumulator.reset();
    }

    @Override
    protected int getBestAction()
    {
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

    private void rollout(RewardGame state, int currentDepth, RewardAccumulator accumulator)
    {
        while(!state.isEnded() && currentDepth <= maxRolloutDepth)
        {
            BaseAction nextAction = new BaseAction(rng.nextInt(NUM_ACTIONS));
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
