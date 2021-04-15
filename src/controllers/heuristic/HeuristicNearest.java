package controllers.heuristic;

import controllers.heuristic.graph.Graph;
import controllers.heuristic.graph.SightPath;
import framework.core.Game;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 22/08/12
 */
public class HeuristicNearest extends HeuristicSolver{

    public int estimateSolutionTime(Game a_game)
    {
        m_graph = new Graph(a_game);
        m_tsp = new TSPGraphNearest(a_game, m_graph);
        m_tsp.solve();
        m_bestRoute = m_tsp.getBestPath();

        int cost = getCost(m_bestRoute, m_graph, a_game);
        //System.out.println("Nearest: " + cost);
        analyseRoute(a_game, m_graph, m_bestRoute);
        return cost;
    }

}
