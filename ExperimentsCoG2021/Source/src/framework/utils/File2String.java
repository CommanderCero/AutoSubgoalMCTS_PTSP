package framework.utils;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Class to manage writing and reading from files.
 * PTSP-Competition
 * Created by Diego Perez, University of Essex.
 * Date: 19/12/11
 */
public class File2String
{
    /**
     * Returns a String with the contents of the file
     * @param filename file name.
     * @return a String with the contents of the file
     */
    public static String get(String filename) {
      try {
        File file = new File(filename);
        byte[] b = new byte[(int) file.length()];
        DataInputStream dis =
          new DataInputStream(new FileInputStream(file));
        dis.readFully(b);
        return new String(b);
      }
      catch(Exception e) {
        System.out.println(e);
        return null;
      }
    }

    /**
     * Reads an input stream and returns its content in a string
     * @param is input stream to read from.
     * @return content of the stream in a string instance.
     */
    public static String read(InputStream is) {
      // dont know the size - therefore
      // use a ByteArrayOutputStream
      String s = null;
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        while ((b = is.read()) != -1)
          bos.write(b);
        return bos.toString();
      }
      catch(Exception e) {}
      return s;
    }

    /**
     * Returns the contents of a file in an array of strings, one per line.
     * @param file filename to read
     * @return An array of strings.
     */
    public static String[][] getArray(String file) {
      try {
        FileInputStream is = new FileInputStream(file);
        String[][] sa = readArray(is);
        is.close();
        return sa;
      }
      catch(Exception e) {
        System.out.println(e);
        return null;
      }
    }

    /**
     * Returns the contents of an input stream in an array of strings, one per line.
     * @param is input stream to read from.
     * @return the array of lines.
     */
    public static String[][] readArray(InputStream is) {
      // dont know the size - therefore
      // use a ByteArrayOutputStream
      String s = null;
      try {
        BufferedReader dis = new BufferedReader(new InputStreamReader(is));
        //DataInputStream dis = new DataInputStream(is);
        Vector v = new Vector();

        while ((s = dis.readLine()) != null) {
          StringTokenizer st = new StringTokenizer(s);
          int n = st.countTokens();
          String[] a = new String[n];
          for (int i=0; i<n; i++)  {
            a[i] = st.nextToken();
          }
          v.addElement(a);
        }
        String[][] sa = new String[v.size()][];
        for (int i=0; i<v.size(); i++)
          sa[i] = (String[]) v.elementAt(i);
        dis.close();
        return sa;
      }
      catch(Exception e) {}
      return null;
    }

    /**
     * Writes a string in a file
     * @param s String to write.
     * @param filename Name of the file to write.
     * @return true if ok.
     */
    public static boolean put(String s, String filename) {
      try {
        return put(s, new File(filename));
      }
      catch(Exception e) {
        System.out.println(e);
        return false;
      }
    }

    /**
     * Writes a string in a file
     * @param s String to write.
     * @param file File object to write to.
     * @return true if ok.
     */
    public static boolean put(String s, File file) {
      try {
        PrintStream ps =
          new PrintStream(new FileOutputStream(file));
        ps.print(s);
        ps.close();
        return true;
      }
      catch(Exception e) {
        System.out.println(e);
        return false;
      }
    }

    /**
     * Writes an array of Strings into a file
     * @param s array of strings.
     * @param filename name of the file to write to.
     * @return if the write was ok.
     */
    public static boolean put(String[] s, String filename) {
        try {
          PrintStream ps =
            new PrintStream(new FileOutputStream(new File(filename)));
            for(int i = 0; i < s.length; ++i)
            {
              ps.println(s[i]);
            }
          ps.close();
          return true;
        }
        catch(Exception e) {
          System.out.println(e);
          return false;
        }
    }

}
