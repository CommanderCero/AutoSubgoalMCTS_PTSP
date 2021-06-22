    package controllers.mcts;

import controllers.heuristic.GameEvaluator;
import framework.core.Controller;
import framework.core.Game;
import framework.core.Waypoint;

import controllers.heuristic.TSPGraphPhysicsEst;
import controllers.heuristic.graph.Path;

import framework.utils.Vector2d;

import java.util.*;

    /**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 26/03/12
 * Time: 16:17
 * To change this template use File | Settings | File Templates.
 */
public class MCTS
{
    /**
     * Root node
     */
    public MCTSNode m_rootNode;

    /**
     * Copy of the game.
     */
    public Game m_currentGameCopy;

    /**
     * Random number generator.
     */
    public Random m_rnd;

    /**
     * Graph for the paths
     */
    public controllers.heuristic.graph.Graph m_graph;

    /**
     *  TSP Graph
     */
    //public TSPGraph m_tspGraph;
    public TSPGraphPhysicsEst m_tspGraph;

    /**
     * Best route found.
     */
    public int[] m_bestRoute;

    /**
     * Path to closest node.
     */
    public Path m_path;

    /**
     * Debug height map
     */
    public int[][] m_heightMap;

    /**
     * Average of executions
     */
    public int m_acumMCTSCount;
    
    /**
     * Number of MCTS executions
     */
    public int m_mctsCount;

    /**
     * Depth of the tree
     */
    public int m_treeDepth;

    /**
     * Average of executions
     */
    public int m_acumTreeDepth;

    /**
     * Obstacle counter.
     */
    public int m_obstacleCounter;

    /**
     * Value for K, rollout depth
     */
    protected static final float C = (float)Math.sqrt(2); //0.025f;
    public static boolean KEEP_TREE=false;
    public static int ROLLOUT_DEPTH = 8;   //10;     //0 for UCT with no rollouts

    //public ArrayList<Vector2d> m_bestRouteSoFar;      //THIS IS DEBUG ONLY
    //public ArrayList<Vector2d> m_currentRoute;        //THIS IS DEBUG ONLY
    public double m_bestScoreSoFar;
    public ArrayList<MacroAction> m_bestActions;
    public ArrayList<MacroAction> m_currentActions;

    public int m_depthDiff;

    public ArrayList<MacroAction> m_actionList;

    public GameEvaluator m_gameEvaluator;

    /**
     * Default constructor.
     */
    public MCTS(Game a_game, long a_timeDue)
    {
        m_rnd = new Random();
        m_graph = new controllers.heuristic.graph.Graph(a_game);
        m_bestRoute = new int[TSPGraph.MAX_NODES];
        m_acumMCTSCount = 0;
        m_treeDepth = 0;
        m_mctsCount = 0;
        m_acumTreeDepth = 0;
        m_depthDiff = 0;
        m_bestScoreSoFar = -Double.MAX_VALUE;

        //m_bestRouteSoFar = new ArrayList<Vector2d>();
        //m_currentRoute = new ArrayList<Vector2d>();
        m_bestActions = new ArrayList<MacroAction>();
        m_currentActions = new ArrayList<MacroAction>();
        m_actionList = new ArrayList<MacroAction>();

        m_tspGraph = new controllers.heuristic.TSPGraphPhysicsEst(a_game, m_graph); //new TSPGraphSight(a_game, m_graph);

	    m_tspGraph.solve();
        //m_tspGraph.assingBestPath(ExecSync.m_nroutes[ExecSync.currenMapIndex]);

        m_bestRoute = m_tspGraph.getBestPath();
        m_gameEvaluator = new GameEvaluator(m_tspGraph, m_graph, true);

        // Create actions
        for(int i = Controller.ACTION_NO_FRONT; i <= Controller.ACTION_THR_RIGHT; ++i)  //6 actions
        //for(int i = Controller.ACTION_THR_FRONT; i <= Controller.ACTION_THR_RIGHT; ++i)   //Only 3 actions
        {
            boolean t = Controller.getThrust(i);
            int s = Controller.getTurning(i);
            m_actionList.add(new MacroAction(t,s,GameEvaluator.MACRO_ACTION_LENGTH));
        }

        /*System.out.println();
        for(int i = 0; i <  10; ++i)
        {
            System.out.print(m_bestRoute[i]);
        }
        System.out.println();             */
    }

    /**
     * Executes the MCTS algorithm
     * @param a_gameCopy Copy of the game state.
     * @param a_timeDue when this is due to end.
     * @return the action to execute.
     */
    public MacroAction execute(Game a_gameCopy, long a_timeDue, boolean a_throwTree)
    {
        m_currentGameCopy = a_gameCopy;
        m_heightMap = new int[m_currentGameCopy.getMap().getMapWidth()][m_currentGameCopy.getMap().getMapHeight()];
        m_gameEvaluator.updateNextWaypoints(a_gameCopy, 2);

        //m_bestRouteSoFar.clear();
        //m_currentRoute.clear();
        m_bestActions.clear();
        m_currentActions.clear();
        m_bestScoreSoFar = -Double.MAX_VALUE;
        boolean check = false;

        if(m_bestActions.size() <= 1)
        {
            m_bestActions.clear();
            m_bestScoreSoFar = -Double.MAX_VALUE;
        }

        //THIS IS FOR DEBUG:
        //m_path = getPathToWaypoint(a_gameCopy,getNextWaypointInPath(a_gameCopy));

        /*int stuckAction = manageStuckLoS(); //manageStuckNaive();
        if(stuckAction != -1)
        {
            m_rootNode = null;
            return stuckAction;
        }           */

        //Create a new node for this iteration.

        KEEP_TREE = !a_throwTree;

        /** KEEP THE TREE? **/
        if(KEEP_TREE)
        {
            //System.out.println("[MCTS] Keeping the tree.");
            ArrayList al = m_currentGameCopy.getShip().getActionList();
            if(al.size() == 0 || m_rootNode == null || m_rootNode.m_children.size() == 0)
            {
                m_rootNode = new MCTSNode(m_currentGameCopy);
                //System.out.println("To Zero: " + al.size() + ", " + m_rootNode + ", " + m_rootNode.m_children.size());
                m_depthDiff=0;
                check = true;
            }else{
                //int lastAction = (Integer) al.get(al.size()-1);
                //m_rootNode = m_rootNode.m_children.get(lastAction);
                //m_rootNode.m_parent = null;
                m_depthDiff++;
            }
        }else{
            //System.out.println("[MCTS] Throwing the tree away.");
            m_rootNode = new MCTSNode(m_currentGameCopy);
            m_treeDepth = 0;
            m_depthDiff=0;
        }

        double avgTimeTaken = 0;
        int loopCount = 1;
        m_mctsCount++;
        mctsLoop();

        double remaining = (a_timeDue-System.currentTimeMillis());
        while(remaining > 10)
        //while(System.currentTimeMillis()+2*(avgTimeTaken/loopCount)<a_timeDue)  //FOR REAL TIME CONSTRAINTS
        //while(loopCount < 200)
        {
            double start = System.currentTimeMillis();
            mctsLoop();
            loopCount++;
            double end = System.currentTimeMillis();
            avgTimeTaken+=(end-start);
            remaining = (a_timeDue-System.currentTimeMillis());
        }

        m_acumMCTSCount += loopCount;
        m_acumTreeDepth += KEEP_TREE ? (m_treeDepth-m_depthDiff) : m_treeDepth ;
        //System.out.println("Tree depth: " + m_treeDepth);
        //System.out.println(loopCount);

        //MCTSNode best = bestAvgChild();
        //MCTSNode best = mostVistiedChild();
        MCTSNode best = bestChild();
        if(best == null)
        {
            return null;
        }

        //MacroAction m = best.m_lastMove;
        //m_currentGameCopy.tick(m.buildAction());
        //if((m_currentGameCopy.getStepsLeft() == 0) || (m_currentGameCopy.getWaypointsVisited() == 10))
        //{
           /* System.out.println("[info] " + m_acumMCTSCount + " " + m_acumTreeDepth + " "+ m_mctsCount
                    + " " + (m_acumMCTSCount/m_mctsCount)+ " " + (m_acumTreeDepth/m_mctsCount) + " " + K + " " + ROLLOUT_DEPTH);     */
        //}

        return best.m_lastMove;
    }

    /**
     * Executes a MCTS iteration.
     */
    private void mctsLoop()
    {
        ArrayList<MCTSNode> visited = new ArrayList<MCTSNode>();
        MCTSNode currentNode = m_rootNode;
        boolean m_end = false;
        double outcome=0;
        MCTSNode newNode;
        int depth = 0;

        while(!m_end)
        {
            //if(currentNode.m_game.isEnded())
            if(m_gameEvaluator.isEndGame(currentNode.m_game))
            {
                outcome = m_gameEvaluator.scoreGame(currentNode.m_game);
                checkBestRoute(outcome);
                m_end = true;
            }
            else
            {
                //Game is not ended.
                if(currentNode.m_visits == 0 && currentNode.m_parent!=null)
                {
                    outcome = rollout(currentNode, depth);
                    checkBestRoute(outcome);
                    m_end = true;
                }else
                {
                    if(currentNode.m_children.size()<currentNode.m_numMoves)
                    {
                        newNode=expand(currentNode);
                    }else{
                        newNode=chooseChildUCB1(currentNode);
                    }
                    depth++;
                    visited.add(newNode);
                    currentNode=newNode;
                }
            }
        }

        for(int i=0;i<visited.size();i++)
        {
            visited.get(i).m_value+=outcome;
            visited.get(i).m_visits++;
        }
        m_rootNode.m_visits++;
    }

    
    private void advanceGame(Game a_game, MacroAction a_move)
    {
        int singleMove = a_move.buildAction();
        boolean end = false;
        for(int i = 0; !end &&  i < a_move.m_repetitions; ++i)
        {
            a_game.tick(singleMove);
            m_heightMap[(int)a_game.getShip().s.x][(int)a_game.getShip().s.y]++;
            end = GameEvaluator.isEndGame(a_game);
            //m_currentRoute.add(a_game.getShip().s.copy());
        }
        m_currentActions.add(a_move);
    }


    private void checkBestRoute(double a_score)
    {
        if(a_score > m_bestScoreSoFar)
        {
            m_bestScoreSoFar = a_score;
            m_bestActions.clear();
            /*m_bestRouteSoFar.clear();
            for(Vector2d v : m_currentRoute)
            {
                m_bestRouteSoFar.add(v);
            }    */
            for(MacroAction i : m_currentActions)
            {
                m_bestActions.add(i);
            }
        }
        //m_currentRoute.clear();
        m_currentActions.clear();
    }

    /**
     * Gets the best child.
     * @return the best child (node).
     */
    private MCTSNode bestChild()
    {
        double[] values=new double[m_rootNode.m_children.size()];

        for(int i=0;i<values.length;i++)
        {
            values[i]= m_rootNode.m_children.get(i).m_value;
        }

		if(values.length==0)
        {
            return null;
        }
        return m_rootNode.m_children.get(argmax(values));
    }


    /**
     * Gets the best child (average).
     * @return the best child (node).
     */
    private MCTSNode bestAvgChild()
    {
        double[] values=new double[m_rootNode.m_children.size()];

        for(int i=0;i<values.length;i++)
        {
            int nMoves = m_rootNode.m_children.get(i).m_numMoves;
            if(nMoves == 0)
                values[i]=0;
            else
                values[i]= m_rootNode.m_children.get(i).m_value / nMoves;
        }

//		if(values.length>0)
        return m_rootNode.m_children.get(argmax(values));
    }

    private MCTSNode mostVistiedChild()
    {
        double[] values=new double[m_rootNode.m_children.size()];

        for(int i=0;i<values.length;i++)
        {
            values[i]= m_rootNode.m_children.get(i).m_numMoves;
        }

//		if(values.length>0)
        return m_rootNode.m_children.get(argmax(values));
    }

    private MCTSNode leadingToBestRoute()
    {
        MacroAction next = m_bestActions.get(0);
        return m_rootNode.m_children.get(next.buildAction());

    }

    /**
     * Creates a new child of the node received by parameter.
     * @param a_node Node parent of the new node to be created.
     * @return the node created.
     */
    private MCTSNode expand(MCTSNode a_node)
    {
        Game newGame=a_node.m_game.getCopy();
        int nthMove=a_node.m_children.size();
        MacroAction move = m_actionList.get(nthMove);
        advanceGame(newGame, move);
        a_node.m_obstacleCounter += newGame.getShip().getCollLastStep() ? 1 : 0;

        MCTSNode child=new MCTSNode(newGame,a_node,m_actionList.get(nthMove));
        a_node.m_children.add(child);

        if(child.m_depth > m_treeDepth)
            m_treeDepth = child.m_depth;

        return child;

    }

    /**
     * TREE SELECTION: Chooses a node between the children of the node given.
     * @param a_node THe node, parent of the children to be chosen.
     * @return the chosen node.
     */
    private MCTSNode chooseChild(MCTSNode a_node)
    {
        double[] values=new double[a_node.m_children.size()];

        double part=Math.log(a_node.m_visits);

        for(int i=0;i<values.length;i++)
        {
            double vis=a_node.m_children.get(i).m_visits;
            double val=a_node.m_children.get(i).m_value;

            //average value, as usual
            values[i]=(val/ vis)+ C *Math.sqrt(part/vis)+m_rnd.nextFloat()*1e-8;
        }

        return a_node.m_children.get(argmax(values));
    }

    private MCTSNode chooseChildUCB1(MCTSNode a_node)
    {
        double[] values=new double[a_node.m_children.size()];
        double part=Math.log(a_node.m_visits + 1);
        double minFitness = 0;
        double maxFitness = GameEvaluator.MAX_FITNESS;

        for(int i=0;i<values.length;i++)
        {
            double vis=a_node.m_children.get(i).m_visits;
            double val=a_node.m_children.get(i).m_value;
            double estimatedValue = val / vis;
            double estimatedValueN = normalise(estimatedValue, minFitness, maxFitness);
            double valAux = part / vis;

            //average value, as usual
            values[i] = estimatedValueN+ C *Math.sqrt(valAux)+m_rnd.nextFloat()*1e-8;
        }

        int nextAction = argmax(values);
        MCTSNode node = a_node.m_children.get(nextAction);
        m_currentActions.add(m_actionList.get(nextAction));
        return node;
    }

    //Normalizes a value between its MIN and MAX.
    public static double normalise(double a_value, double a_min, double a_max)
    {
        double value = (a_value - a_min)/(a_max - a_min);
        //System.out.println(a_min + "<" + a_value + "<" + a_max + " => " + value);
        return value;
    }

    /**
     * Performs a rollout until the end of the game or a maximum depth reached.
     * @param a_node Node to start the rollout from.
     * @return score value of the game after the rollout.
     */
    private double rollout(MCTSNode a_node, int a_startingDepth)
    {
        Game newGame=null;
        int count=a_startingDepth;
        m_obstacleCounter = a_node.m_obstacleCounter;
        try{
            newGame=a_node.m_game.getCopy();
            boolean gameEnded = m_gameEvaluator.isEndGame(newGame);

            //while(count++<ROLLOUT_DEPTH && !newGame.isEnded())
            while(count++<ROLLOUT_DEPTH && !gameEnded)
            {
                int action=m_rnd.nextInt(Controller.NUM_ACTIONS);
                MacroAction ma = m_actionList.get(action);
                advanceGame(newGame, ma);
                m_obstacleCounter += newGame.getShip().getCollLastStep() ? 1 : 0;
                gameEnded = m_gameEvaluator.isEndGame(newGame);
            }

           /* if(gameEnded)
                System.out.println("Game ended at: " + (count-1) + " / " + ROLLOUT_DEPTH); */

            double value = m_gameEvaluator.scoreGame(newGame);
            return value;

        }catch(Exception e)
        {
            throw new RuntimeException(e.toString());
        }
    }


    private Vector2d getVisiblePointInPath(Game a_game,controllers.heuristic.graph.Path a_p)
    {
        int numPoints = a_p.m_points.size();
        int positionToCheck = numPoints-1;
        int numDivisions = 1;
        boolean end = false;
        Vector2d interPos = new Vector2d();
        while (!end && numDivisions < 6)
        {
            controllers.heuristic.graph.Node n = m_graph.getNode(a_p.m_points.get(positionToCheck));
            interPos.x = n.x();
            interPos.y = n.y();

            boolean lineOfSight = a_game.getMap().LineOfSight(a_game.getShip().s,interPos);
            if(lineOfSight)
            {
                //This is the one.
                end = true;
            }

            //If not, come closer.
            numDivisions++;
            positionToCheck /= 2;
        }
        if(end)
            return interPos;
        else return null;
    }

    private Vector2d getBestTargetPointInPath(Game a_game, Waypoint a_closestW, controllers.heuristic.graph.Path a_p)
    {
        Vector2d interPos = getVisiblePointInPath(a_game, a_p);

        //Not possible, just go for next point in path.
        if(interPos == null)
        {
            if(a_p.m_points.size()>1)
            {
                int which = a_p.m_points.get(1); //1 makes sure that it is ahead.
                controllers.heuristic.graph.Node n = m_graph.getNode(which);
                interPos = new Vector2d(n.x(), n.y());
            }else
            {
                //This is a bit annoying. Just return the position of the waypoint.
                interPos = a_closestW.s;
            }
        }
        return interPos;
    }

    
    private boolean match(ArrayList<Integer> a_followedOrder, int[] a_pathDesired)
    {
        int idx = 0;
        for (Integer i : a_followedOrder)
        {
            if(i != a_pathDesired[idx])
                return false;
            idx++;
        }
        return true;
    }
    
    private controllers.heuristic.graph.Path getPathToWaypoint(Game a_game, Waypoint a_closestW)
    {
        controllers.heuristic.graph.Node shipNode = m_graph.getClosestNodeTo(a_game.getShip().s.x, a_game.getShip().s.y);
        controllers.heuristic.graph.Node waypointNode = m_graph.getClosestNodeTo(a_closestW.s.x, a_closestW.s.y);
        return  m_graph.getPath(shipNode.id(), waypointNode.id());
    }
    
    
    private Waypoint getClosestWaypoint(Game a_gameCopy)
    {
        Waypoint w = null;
        double minDistance = Double.MAX_VALUE;
        for(Waypoint way: a_gameCopy.getWaypoints())
        {
            if(!way.isCollected())     //Only consider those not collected yet.
            {
                double fx = way.s.x-a_gameCopy.getShip().s.x, fy = way.s.y-a_gameCopy.getShip().s.y;
                double dist = Math.sqrt(fx*fx+fy*fy);
                if( dist < minDistance )
                {
                    //Keep the minimum distance.
                    minDistance = dist;
                    w = way;
                }
            }
        }

        return w;
    }


    public Waypoint getNextWaypointInPath(Game a_gameCopy)
    {
        try{
        ArrayList<Waypoint> waypoints = a_gameCopy.getWaypoints();
        int nVisited = a_gameCopy.getWaypointsVisited();
        if(nVisited == waypoints.size())
            return null;

        int next = m_tspGraph.getBestPath()[nVisited];
        return waypoints.get(next);
        }catch(Exception e){
            int a  = 0;
        }
        return null;
    }

    public int[] getNextWaypointsInPath (Game a_gameCopy, int a_howMany)
    {
        int[] w;
        try{
            ArrayList<Waypoint> waypoints = a_gameCopy.getWaypoints();

            int nVisited = a_gameCopy.getWaypointsVisited();
            if(nVisited == waypoints.size())
                return null;

            w = new int[Math.min(a_howMany, waypoints.size() - nVisited)];

            for(int i = 0; i < w.length; ++i)
            {
                w[i] = m_tspGraph.getBestPath()[nVisited];
                //w[i] = waypoints.get(next);
            }
            return w;

        }catch(Exception e){
            int a  = 0;
        }
        return null;
    }


    public int argmax(double... sequence)
    {
        try{
            int index=0;
            double max=sequence[index];

            for(int i=0;i<sequence.length;i++)
                if(Double.compare(sequence[i],max)>0)
                {
                    max=sequence[i];
                    index=i;
                }

            return index;
        }
        catch(Exception e)
        {
//			System.out.println("ERROR");
            return 0;
        }
    }

}
