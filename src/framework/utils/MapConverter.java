package framework.utils;

import framework.core.Waypoint;

import java.util.LinkedList;

/**
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 21/12/11
 */
public class MapConverter
{
    public static final char EDGE = 'E';
    public static final char START = 'S';
    public static final char COLL = 'C';
    public static final char NIL = '.';

    private char m_mapChar[][];
    private char m_mapCharBoundaries[][];
    private String m_filenameIn;
    private String m_filenameOut;
    public Vector2d m_startPoint;
    public LinkedList<Waypoint> m_collectables;
    public int SCALE_FACTOR = 1;

    public MapConverter(String a_filenameIn, String a_filenameOut, int scaleFactor)
    {
        m_filenameIn = a_filenameIn;
        m_filenameOut = a_filenameOut;
        SCALE_FACTOR = scaleFactor;
        m_startPoint = new Vector2d();
        m_collectables = new LinkedList<Waypoint>();
        readMap();
        writeMap();
    }


    private void readMap()
    {
        String[][] fileData = File2String.getArray(m_filenameIn);
        int height = 0;
        int width = 0;
        char[][] mapChar = null;

        int x = 0, xInMap = 0;
        String[] line;
        while(x < fileData.length)
        {
            line = fileData[x]; //Get following line.

            String first = line[0];
            if(first.equalsIgnoreCase("type"))
            {
                //Ignore
            }else if(first.equalsIgnoreCase("height"))
            {
                String h = line[1];
                height = Integer.parseInt(h);
            }
            else if(first.equalsIgnoreCase("width"))
            {
                String w = line[1];
                width = Integer.parseInt(w);
            }
            else if(first.equalsIgnoreCase("map"))
            {
                //Ignore ... but time to create the map
                mapChar = new char[width][height];
            }
            else
            {
                //MAP INFORMATION
                String lineStr = line[0];
                int yInMap = 0;
                int yInFile = 0;
                while(yInMap < lineStr.length())
                {
                    char data = lineStr.charAt(yInFile);
                    //System.out.println(xInMap + "," + yInMap + ": " + data);
                    mapChar[yInMap][xInMap] = data;

                    ++yInMap;
                    ++yInFile;
                }
                ++xInMap;
            }

            ++x;
        }

        //Post-process: Scale.
        scaleMap(mapChar, SCALE_FACTOR);

        setBoundaries();
    }


    private void scaleMap(char[][] a_map, int a_scaleFactor)
    {
        //Each row, duplicate it a_scaleFactor times.
        m_mapChar = new char[a_map.length*a_scaleFactor][a_map[0].length*a_scaleFactor];
        char[] rowBuffer = new char[a_map[0].length]; //To copy elements BEFORE expand the row.

        for(int i = 0; i < a_map.length; i++)
        {
            for(int j = 0; j < a_map[0].length; ++j)
            {
                rowBuffer[j] = a_map[i][j];
                for(int s = 0; s < a_scaleFactor; s++)
                {
                    int row = i*a_scaleFactor+s;
                    extendRow(row,a_scaleFactor, rowBuffer);
                }
            }

        }
    }

    private void extendRow(int a_row, int a_scaleFactor, char[] a_rowBuffer)
    {
        int yInArray=0, i=0;
        for(i = 0; i < a_rowBuffer.length; i++)
        {
            char data = a_rowBuffer[i];
            for(int s = 0; s < a_scaleFactor; s++)
            {
                yInArray = i*a_scaleFactor+s;
                if(isLogic(data))
                    m_mapChar[a_row][yInArray] = NIL;
                else
                    m_mapChar[a_row][yInArray] = data;

            }
        }
    }


    private void setBoundaries()
    {
        m_mapCharBoundaries = new char[m_mapChar.length + 2][m_mapChar[0].length + 2];
        int width = m_mapCharBoundaries.length;
        int height = m_mapCharBoundaries[0].length;

        //Set boundaries in the edges:
        for(int i = 0; i < m_mapCharBoundaries[0].length; ++i)
        {
            m_mapCharBoundaries[0][i] = EDGE;         //Up
            m_mapCharBoundaries[width-1][i] = EDGE;   //Down
        }

        for(int i = 0; i < m_mapCharBoundaries.length; ++i)
        {
            m_mapCharBoundaries[i][0] = EDGE;         //LEFT
            m_mapCharBoundaries[i][height-1] = EDGE;   //RIGHT
        }

        //Now, copy the contents

        for(int i = 0; i < m_mapChar.length; i++)
        {
            for(int j = 0; j < m_mapChar[0].length; ++j)
            {
                m_mapCharBoundaries[i+1][j+1] = m_mapChar[i][j];
            }
        }
    }

    public boolean isLogic(char data)
    {
        if(data == START || data == COLL)
            return true;
        return false;
    }


    private void writeMap()
    {
        StringBuffer sb = new StringBuffer();
        String[] lines = new String[m_mapCharBoundaries[0].length+4];

        lines[0] = "type octile";
        lines[1] = "height " + m_mapCharBoundaries[0].length;
        lines[2] = "width " + m_mapCharBoundaries.length;
        lines[3] = "map";

        for(int i = 0; i < m_mapCharBoundaries[0].length; i++)
        {
            for(int j = 0; j < m_mapCharBoundaries.length; ++j)
            {
                char data = m_mapCharBoundaries[j][i];
                sb.append(data);
            }
            lines[i+4] = sb.toString();
            sb = new StringBuffer();
        }

        File2String.put(lines,m_filenameOut);

    }


    public static void main(String args[])
    {
        if(args.length != 3)
        {
            System.out.println("Incorrect number of arguments. Usage: ");
            System.out.println(" java MapConverter <originalMapFile> <destinationMapFile> <scaleFactor>");
            System.out.println(" where: ");
            System.out.println("   <originalMapFile>      Path to the file to be scaled.");
            System.out.println("   <destinationMapFile>   File to save the scaled file.");
            System.out.println("   <scaleFactor>          Factor to scale the map.");
        }else new MapConverter(args[0], args[1], Integer.parseInt(args[2])) ;
    }

}
