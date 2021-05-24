package controllers.autoSubgoalMCTS;

import controllers.autoSubgoalMCTS.RewardGames.NaiveRewardGame;
import controllers.autoSubgoalMCTS.RewardGames.RewardGame;
import controllers.autoSubgoalMCTS.SubgoalPredicates.PositionGridPredicate;
import controllers.autoSubgoalMCTS.SubgoalSearch.MCTSNoveltySearch.MCTSNoveltySearch;
import controllers.autoSubgoalMCTS.SubgoalSearch.RandomPredicateSearch.RandomPredicateSearch;
import framework.core.Controller;
import framework.core.Game;
import framework.utils.Vector2d;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class AutoSubgoalController extends AbstractController
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
    public AutoSubgoalController(Game game, long dueTimeMs)
    {
        PositionGridPredicate predicate = new PositionGridPredicate(25, 5);
        //RandomPredicateSearch.treatHorizonStatesAsSubgoals = false;
        //RandomPredicateSearch subgoalSearch = new RandomPredicateSearch(predicate, 5, 400, rng);
        MCTSNoveltySearch subgoalSearch = new MCTSNoveltySearch(4, new PositionBehaviourFunction(), rng);
        algorithm = new AutoSubgoalMCTS(subgoalSearch, 300, rng);
    }

    @Override
    protected void step(RewardGame game)
    {
        algorithm.step(game);
    }

    @Override
    protected int getBestAction()
    {
        int action = algorithm.getNextAction();
        return action == -1 ? rng.nextInt(NUM_ACTIONS) : action;
    }

    @Override
    public synchronized void paint(Graphics2D graphics)
    {
        graphics.setColor(Color.yellow);
        drawSubgoals(graphics, algorithm.getRoot());
        //PositionGridPredicate predicate = new PositionGridPredicate(25, 5);
        //predicate.render(graphics, lastState);
    }

    private void drawSubgoals(Graphics2D graphics, MCTSNode<SubgoalData> node)
    {
        if(node.data.lastSeenPosition == null)
            return;

        int r = 2;
        graphics.fillOval((int)node.data.lastSeenPosition.x - r, (int)node.data.lastSeenPosition.y - r, 2 * r, 2 * r);

        for(int i = 0; i < node.children.size(); i++)
        {
            MCTSNode<SubgoalData> child = node.children.get(i);
            if(child.data.lastSeenPosition == null)
                continue;


            graphics.drawLine((int)node.data.lastSeenPosition.x, (int)node.data.lastSeenPosition.y, (int)child.data.lastSeenPosition.x, (int)child.data.lastSeenPosition.y);
            drawSubgoals(graphics, child);
        }
    }
}
