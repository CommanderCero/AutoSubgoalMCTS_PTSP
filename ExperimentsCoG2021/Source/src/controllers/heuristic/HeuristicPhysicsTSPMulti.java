package controllers.heuristic;

import controllers.heuristic.graph.Graph;
import framework.core.Game;

import java.util.NavigableSet;
import java.util.TreeMap;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 22/08/12
 */
public class HeuristicPhysicsTSPMulti extends HeuristicSolver{

    public int estimateSolutionTime(Game a_game)
    {
        int costTarget = 2; //n-th of the secondary routes.
        m_graph = new Graph(a_game);
        m_tsp = new TSPGraphPhysicsEst(a_game, m_graph);
        m_tsp.solve();
        m_bestRoute = m_tsp.getBestPath();
        int bestCost = getCost(m_bestRoute, m_graph, a_game);

        int[] array = new int[m_bestRoute.length];

        m_tsp.improveOpt();

        TreeMap<Integer, Integer[]> paths = ((TSPGraphPhysicsEst) m_tsp).m_paths;
        NavigableSet<Integer> keySet = paths.navigableKeySet();

        /*Integer one = keySet.pollFirst();
        Integer two = keySet.pollFirst();
        Integer three = keySet.pollFirst();
        Integer four = keySet.pollFirst();
        Integer five = keySet.pollFirst();  */

        Object[] keys = keySet.toArray();

        Integer one = (Integer) keys[0];
        Integer two = (Integer) keys[1];
        Integer three = (Integer) keys[2];
        Integer four = (Integer) keys[3];
        Integer five = (Integer) keys[4];

        System.out.println("best: " + bestCost);
        analyseRoute(a_game, m_graph, m_bestRoute);

        System.out.println("one: " + one);
        for(int j = 0; j < paths.get(one).length; ++j) array[j] = paths.get(one)[j];
        analyseRoute(a_game, m_graph, array);

        System.out.println("two: " + two);
        for(int j = 0; j < paths.get(two).length; ++j) array[j] = paths.get(two)[j];
        analyseRoute(a_game, m_graph, array);

        System.out.println("three: " + three);
        for(int j = 0; j < paths.get(three).length; ++j) array[j] = paths.get(three)[j];
        analyseRoute(a_game, m_graph, array);


        System.out.println("four: " + four);
        for(int j = 0; j < paths.get(four).length; ++j) array[j] = paths.get(four)[j];
        analyseRoute(a_game, m_graph, array);


        System.out.println("five: " + five);
        for(int j = 0; j < paths.get(five).length; ++j) array[j] = paths.get(five)[j];
        analyseRoute(a_game, m_graph, array);

        return two;
    }

}
