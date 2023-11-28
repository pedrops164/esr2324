package Common;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class Node {
    protected int id;
    protected String ip;
    protected String RPIP;
    protected String logFile;
    public Logger logger;
    protected Map<Integer, String> neighbours;
    
    public Node(int id, NeighbourReader nr, boolean debugMode)
    {
        this.id = id;
        this.logFile = "/home/ESRproj/logs/node" + this.id + ".log"; 
        this.logger = new Logger(this.logFile, debugMode);
        
        try 
        {
            File log = new File(this.logFile);
            if (!log.exists())
            {
                File dir = new File("/home/ESRproj/logs/");
                if (!dir.exists())
                    dir.mkdirs();
                if (log.createNewFile())
                    this.logger.log(new LogEntry("Log File created"));
                else
                    this.logger.log(new LogEntry("Couldn't create Log File"));
            }

            this.neighbours = nr.readNeighbours();
            this.logger.log(new LogEntry("Read Neighbours"));
            this.RPIP = nr.getRPString();
            this.logger.log(new LogEntry("Read Rendezvous Point IP Address"));
            this.ip = this.neighbours.get(id);
            this.logger.log(new LogEntry("Read own Address"));
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public Node(int id, NeighbourReader nr, String logFile, boolean debugMode)
    {
        this.id = id;
        this.logFile = logFile;
        this.logger = new Logger(this.logFile, debugMode);
        
        try 
        {
            File log = new File(this.logFile);
            if (!log.exists())
            {
                if (log.createNewFile())
                    this.logger.log(new LogEntry("Log File created"));
                else
                    this.logger.log(new LogEntry("Couldn't create Log File"));
            }

            this.neighbours = nr.readNeighbours();
            this.logger.log(new LogEntry("Read Neighbours"));
            this.RPIP = nr.getRPString();
            this.logger.log(new LogEntry("Read Rendezvous Point IP Address"));
            this.ip = this.neighbours.get(id);
            this.logger.log(new LogEntry("Read own Address"));
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public Map<Integer, String> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(Map<Integer, String> neighbours) {
        this.neighbours = neighbours;
    }

    public void log(LogEntry entry) throws IOException
    {
        this.logger.log(entry);
    }

    public String getNeighbourIp(int neighbourId) {
        return this.neighbours.get(neighbourId);
    }
}
