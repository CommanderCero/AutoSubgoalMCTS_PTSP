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

public class VanillaMCTS extends AbstractController
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

    public static double explorationRate = 10;
    public static int maxRolloutDepth = 30;

    private MCTSNode<MCTSData> root;
    private RewardAccumulator accumulator;

    public VanillaMCTS(Game game, long dueTimeMs)
    {
        root = new MCTSNode<MCTSData>(new MCTSData(-1, game.getShip().s));
        // Initialize reward accumulator
        accumulator = new RewardAccumulator(0.99);
    }

    @Override
    protected void step(RewardGame game)
    {
        // Selection
        int depth = 0;
        MCTSNode<MCTSData> currNode = root;
        while (currNode.children.size() == NUM_ACTIONS && depth < maxRolloutDepth)
        {
            currNode = currNode.selectUCT(explorationRate, rng);
            accumulator.addReward(currNode.data.action.apply(game));
            depth++;
        }

        if (!game.isEnded() && depth < maxRolloutDepth)
        {
            // Expansion
            int nextAction = currNode.children.size();
            MCTSData newData = new MCTSData(nextAction, game.getState().getShip().s);
            currNode = currNode.addChild(newData);
            depth++;

            if(depth > maxRolloutDepth)
            {
                System.out.println("Error");
            }

            // Simulation
            rollout(game, depth, accumulator);
        }

        // Backpropagation
        currNode.backpropagate(accumulator.getRewardSum());
        accumulator.reset();
    }

    @Override
    protected BaseAction getBestAction()
    {
        MCTSNode<MCTSData> bestChild = root.getChildWithHighestReturn(rng);
        root = bestChild;
        root.parent = null;
        return root.data.action;
    }

    private void rollout(RewardGame state, int currentDepth, RewardAccumulator accumulator)
    {
        while(!state.isEnded() && currentDepth <= maxRolloutDepth)
        {
            BaseAction nextAction = new BaseAction(rng.nextInt(NUM_ACTIONS));
            accumulator.addReward(nextAction.apply(state));

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
