package Common;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Common.TCPConnection.Packet;

public abstract class Node {
    protected int id;
    protected String ip;
    protected List<String> RPIPs;
    protected String bootstrapperIP;
    protected String logFile;
    protected Logger logger;
    protected Map<Integer, String> neighbours;
    
    public Node(int id, boolean debugMode, String bootstrapperIP)
    {
        this.id = id;
        this.logger = new Logger(this.logFile, debugMode);
        this.createDefaultLogFile();
        this.bootstrapperIP = bootstrapperIP;
        this.neighbours = new HashMap<>();
        this.RPIPs = null;
        this.ip = null;
    }

    public Node(int id, String logFile, boolean debugMode, String bootstrapperIP)
    {
        this.id = id;
        this.logFile = logFile;
        this.createGivenLogFile();
        this.logger = new Logger(this.logFile, debugMode);
        this.bootstrapperIP = bootstrapperIP;
        this.neighbours = new HashMap<>();
        this.RPIPs = null;
        this.ip = null; 
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        int oldID = this.id;
        this.id = id;
        if (oldID == -1)
        {
            this.createDefaultLogFile();
            this.logger.setLogFile(this.logFile);
        }
    }

    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getBootstrapperIP() {
        return bootstrapperIP;
    }

    public void setBootstrapperIP(String bootstrapperIP) {
        this.bootstrapperIP = bootstrapperIP;
    }
    
    public Map<Integer, String> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(Map<Integer, String> neighbours) {
        this.neighbours = neighbours;
    }

    public void log(LogEntry entry) {
        try {
            this.logger.log(entry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getNeighbourIp(int neighbourId) {
        return this.neighbours.get(neighbourId);
    }

    public List<String> getNeighboursIps(List<Integer> neighborIds){
        List<String> ips = new ArrayList<>();
        
        for(Integer id : neighborIds){
            ips.add(this.neighbours.get(id));
        }
        return ips;
    }

    public void createDefaultLogFile()
    {
        this.logFile = "./logs/" + this.id + ".log"; 
        if (this.id != -1)
        {
            try 
            {
                File log = new File(this.logFile);
                if (!log.exists())
                {
                    File dir = new File("./logs/");
                    if (!dir.exists())
                    dir.mkdirs();
                    if (log.createNewFile())
                    this.logger.log(new LogEntry("Log File created"));
                    else
                    this.logger.log(new LogEntry("Couldn't create Log File"));
                }
                
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }

    public void createGivenLogFile()
    {
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

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public boolean messageBootstrapper()
    {
        int maxRetry = 10;
        int iters = 0;
        boolean successfull = false;
        do
        {
            try 
            {
                Socket socket = new Socket(this.bootstrapperIP, 333);
                TCPConnection tcpConnection = new TCPConnection(socket);
                tcpConnection.send(4, "getNeighbours".getBytes());
    
                Packet  pID = tcpConnection.receive(),
                        pRPIPs = tcpConnection.receive(),
                        pNeighbours = tcpConnection.receive();
                
                this.id = Utility.deserializeInt(pID.data);
                
                List<?> aux = (List<?>)Utility.deserializeObject(pRPIPs.data);
                this.RPIPs = aux.stream().map(s -> (String)s).toList();
    
                Map<?,?> auxMap = (Map<?,?>)Utility.deserializeObject(pNeighbours.data);
                for (Map.Entry<?,?> entry : auxMap.entrySet())
                {
                    this.neighbours.put((Integer)entry.getKey(), (String)entry.getValue());
                }
    
                tcpConnection.send(8, "OK".getBytes());
                this.log(new LogEntry("Received id, RP information and neighbours from Bootstrapper"));
                successfull = true;
            } 
            catch (IOException e) 
            {
                this.log(new LogEntry("Error connecting to bootstrapper. Retrying in 5 seconds."));
                iters++;
                try 
                {
                    Thread.sleep(5000);
                } 
                catch (InterruptedException e1) 
                {
                    e1.printStackTrace();
                }
            }
        } while(!successfull && iters<maxRetry);
        return successfull;
    }
}
