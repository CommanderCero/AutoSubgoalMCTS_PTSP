package controllers.heuristic;

import controllers.heuristic.graph.Graph;
import framework.core.Game;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 22/08/12
 */
public class HeuristicNormalTSP extends HeuristicSolver{

    public int estimateSolutionTime(Game a_game)
    {
        m_graph = new Graph(a_game);
        m_tsp = new TSPGraphNormal(a_game, m_graph);
        m_tsp.solve();
        m_bestRoute = m_tsp.getBestPath();

        int cost = getCost(m_bestRoute, m_graph, a_game);
        //System.out.println("Normal: " + cost);
        analyseRoute(a_game, m_graph, m_bestRoute);
        return cost;
    }
}
