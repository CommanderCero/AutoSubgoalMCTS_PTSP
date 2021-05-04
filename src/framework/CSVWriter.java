package framework;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class CSVWriter implements AutoCloseable
{
    public CSVWriter(String filePath, String[] header) throws IOException
    {
        this.header = header;
        writer = new FileWriter(filePath);
        for(String columnHeader : header)
        {
            append(columnHeader);
        }
    }

    public void append(String content) throws IOException
    {
        writer.append(content);
        currentIndex++;
        if(currentIndex == header.length)
        {
            writer.append(newLine);
            currentIndex = 0;
        }
        else
        {
            writer.append(delimiter);
        }
    }

    public void append(int value) throws IOException
    {
        append(Integer.toString(value));
    }

    public void close() throws IOException
    {
        writer.close();
    }

    private String[] header;
    private int currentIndex = 0;
    private char delimiter = '\t';
    private char newLine = '\n';
    private FileWriter writer;
}
