package Common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Common.TCPConnection.Packet;

public abstract class Node {
    protected int id;
    protected List<String> ips;
    protected List<String> RPIPs;
    protected String bootstrapperIP;
    protected String logFile;
    protected Logger logger;
    protected Map<Integer, String> neighbours;
    
    public Node(int id, boolean debugMode, String bootstrapperIP)
    {
        this.id = id;
        this.logFile = null;
        this.logger = new Logger(this.logFile, debugMode);
        this.createDefaultLogFile();
        this.logger.setLogFile(this.logFile);
        this.bootstrapperIP = bootstrapperIP;
        this.neighbours = new HashMap<>();
        this.RPIPs = null;
        this.ips = new ArrayList<>();
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
        this.ips = new ArrayList<>();
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

    public List<String> getIps() {
        return ips;
    }
    
    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    public String getBootstrapperIP() {
        return bootstrapperIP;
    }

    public void setBootstrapperIP(String bootstrapperIP) {
        this.bootstrapperIP = bootstrapperIP;
    }

    public List<String> getRPIPs()
    {
        return this.RPIPs;
    }

    public void setRPIPs(List<String> rpips)
    {
        this.RPIPs = rpips;
    }
    
    public Map<Integer, String> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(Map<Integer, String> neighbours) {
        this.neighbours = neighbours;
    }

    public void log(LogEntry logEntry) {
        try {
            this.logger.log(logEntry);
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
                    boolean created = false;
                    if (log.createNewFile())
                        created = true;    
                    new FileWriter(log, false).close();

                    if (created)
                        this.logger.log(new LogEntry("Log File created"));
                    else
                        this.logger.log(new LogEntry("Couldn't create Log File"));
                }
                else
                    new FileWriter(log, false).close();
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
                boolean created = false;
                if (log.createNewFile())
                    created = true;    
                new FileWriter(log, false).close();

                if (created)
                    this.logger.log(new LogEntry("Log File created"));
                else
                    this.logger.log(new LogEntry("Couldn't create Log File"));
            }
            else
                new FileWriter(log, false).close();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public int messageBootstrapper()
    {
        int maxRetry = 10;
        int iters = 0;
        int successfull = 1;
        do
        {
            try 
            {
                Socket socket = new Socket(this.bootstrapperIP, 333);
                TCPConnection tcpConnection = new TCPConnection(socket);
                tcpConnection.send(4, "getNeighbours".getBytes());

                Packet validationPacket = tcpConnection.receive();
                if (new String(validationPacket.data).equals("invalid"))
                {
                    return 2;
                }
                
                tcpConnection.send(4, "OK".getBytes());
    
                Packet  pID = tcpConnection.receive(),
                        pIPs = tcpConnection.receive(),
                        pRPIPs = tcpConnection.receive(),
                        pNeighbours = tcpConnection.receive();
                
                this.setId(Util.deserializeInt(pID.data));

                List<?> auxIps = (List<?>)Util.deserializeObject(pIPs.data);
                this.setIps(auxIps.stream().map(s->(String)s).toList());
                
                List<?> aux = (List<?>)Util.deserializeObject(pRPIPs.data);
                this.RPIPs = aux.stream().map(s -> (String)s).toList();
    
                Map<?,?> auxMap = (Map<?,?>)Util.deserializeObject(pNeighbours.data);
                for (Map.Entry<?,?> entry : auxMap.entrySet())
                {
                    this.neighbours.put((Integer)entry.getKey(), (String)entry.getValue());
                }
    
                tcpConnection.send(4, "OK".getBytes());
                this.log(new LogEntry("Received id, RP information and neighbours from Bootstrapper"));
                successfull = 0;
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
        } while(successfull!=0 && iters<maxRetry);
        return successfull;
    }
}
