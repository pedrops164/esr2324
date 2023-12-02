package Common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used as a Logger in the context of the project.
 */
public class Logger 
{
    /**
     * String containing the name of the log file
     */
    private String logFile;
    /**
     * boolean indicating whether the logs should be displayed on the terminal or file
     */
    private boolean debugMode;
    /**
     * {@link List} of staged entries
     */
    private List<LogEntry> stagedEntries;

    /**
     * Constructs a new CCLoger instance
     * @param logFile normal log file of the server
     * @param debugMode boolean indicating whether debug mode is enabled
     */
    public Logger (String logFile, boolean debugMode)
    {
        this.logFile = logFile;
        this.debugMode = debugMode;
        this.stagedEntries = new ArrayList<LogEntry>();
    }

    /**
     * Method used to set the normal log file
     * @param logFile Name of the log file
     */
    public synchronized void setLogFile(String logFile)
    {
        this.logFile = logFile;
    }
    
    /**
     * Method used to log a {@link LogEntry}
     * @param log Entry to be logged
     * @throws IOException if there's an error writing to the log file
     */
    public synchronized void log(LogEntry log)
    {
        if (this.debugMode)
            System.out.print(log);

        if (this.logFile == null || !new File(this.logFile).exists())
            this.stagedEntries.add(log);
        else
        {
            try 
            {
                this.stagedEntries.add(log);
                FileWriter fw = new FileWriter(this.logFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                for (LogEntry entry : stagedEntries)
                bw.write(entry.toString());
                bw.close();
                fw.close();
                this.stagedEntries.clear();
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }
}