package Common;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
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
        this.logFile = "./logs/" + this.id + ".log"; 
        this.logger = new Logger(this.logFile, debugMode);
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
            
            this.bootstrapperIP = bootstrapperIP;
            this.neighbours = null;
            this.RPIPs = null;
            this.ip = null;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public Node(int id, String logFile, boolean debugMode, String bootstrapperIP)
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

            this.bootstrapperIP = bootstrapperIP;
            this.neighbours = null;
            this.RPIPs = null;
            this.ip = null;
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

    public boolean messageBootstrapper()
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
            this.RPIPs = (List<String>)Utility.deserializeObject(pRPIPs.data);
            this.neighbours = (Map<Integer,String>)Utility.deserializeObject(pNeighbours.data);

            tcpConnection.send(8, "OK".getBytes());
            this.log(new LogEntry("Received ip, RP information and neighbours from Bootstrapper"));
            return true;
        } 
        catch (Exception e) 
        {
            this.log(new LogEntry("Error connecting to bootstrapper.."));
        }
        return false;
    }
}
