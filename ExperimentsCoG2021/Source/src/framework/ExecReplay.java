package framework;
import framework.core.*;
import framework.utils.JEasyFrame;

/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement his controller in a subpackage of 'controllers'.
 * The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class ExecReplay extends Exec
{
    /**
     * Name of the map tp execute the reply in.
     */
    protected static String m_mapName;
    
    /**
     * Replay a previously saved game.
     * @param visual Indicates whether or not to use visuals
     * @param delay The delay between time-steps
     */
    public static void replayGame(boolean visual, int delay)
    {
        ///Actions to execute.
        int[] actionsToExecute;

        try
        {
            actionsToExecute = readForces(m_actionFilename);

            //Create the game instance.
            m_game = new Game(m_mapName);

            m_game.go();
            m_game.getShip().setStarted(true);

            //Indicate what are we running
            if(m_verbose) System.out.println("Running actions from " + m_actionFilename + " in map " + m_game.getMap().getFilename() + "...");

            JEasyFrame frame;
            if(visual)
            {
                //View of the game, if applicable.
                m_view = new PTSPView(m_game, m_game.getMapSize(), m_game.getMap(), m_game.getShip(), m_controller);
                frame = new JEasyFrame(m_view, "PTSP-Game Replay: " + m_actionFilename);
            }

            for(int j=0;j<actionsToExecute.length;j++)
            {
                m_game.tick(actionsToExecute[j]);

                waitStep(delay);

                if(visual)
                    m_view.repaint();
            }

            if(m_verbose)
                m_game.printResults();

        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    /**
     * The main method. Several options are listed - simply remove comments to use the option you want.
     *
     * @param args the command line arguments. Not needed in this class.
     */
    public static void main(String[] args)
    {

        m_mapName = "maps/StageA/ptsp_map01.map";  //Set here the name of the map to play in.
        m_actionFilename = "route_20-Mar-2012_14.45.47.txt"; //Indicate here the name of the file with the actions saved TO SEE a replay.
        m_visibility = true; //Set here if the graphics must be displayed or not (for those modes where graphics are allowed).
        m_verbose = true;

        /////// 1. Executes a replay.
        /////// Note: using a delay of 0 with m_visibility=true will let you see close to nothing. We recommend delay=2 instead.
        int delay=5; //2: quickest, PTSPConstants.DELAY: human play speed, PTSPConstants.ACTION_TIME_MS: max. controller delay
        replayGame(m_visibility, delay);
    }


}