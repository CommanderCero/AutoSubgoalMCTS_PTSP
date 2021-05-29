package controllers.heuristic;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 22/08/12
 */
public abstract class TSPSolver {

    public abstract void solve();
    public abstract int[] getBestPath();
    public abstract void improveOpt();
}
