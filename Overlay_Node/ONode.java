package Overlay_Node;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.LivenessCheckWorker;
import Common.Node;
import Common.PathNode;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.UDPDatagram;
import Common.Utility;
import Common.VideoMetadata;
import Common.TCPConnection.Packet;
import Common.NormalFloodWorker;
import Common.Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class ONode extends Node {
    private Map<String, List<Integer>> streamNeighours;
    private BootstrapperHandler bootstrapperHandler;
    private String bootstrapConfigFile;

    public ONode(String bootstrapperIP, boolean debugMode)
    {
        super(-1, debugMode, bootstrapperIP);
        this.streamNeighours = new HashMap<>();
        this.bootstrapperHandler = null;
    }

    public ONode(int id, String bootstrapConfigFile, boolean debugMode)
    {
        super(id, debugMode, "localhost");
        this.streamNeighours = new HashMap<>();
        this.bootstrapConfigFile = bootstrapConfigFile;
        this.bootstrapperHandler = new BootstrapperHandler(this.bootstrapConfigFile);
        this.neighbours = this.bootstrapperHandler.getNeighboursFromID(id);
        this.RPIPs = this.bootstrapperHandler.getRPIPs();
    }

    public int getId()
    {
        return id;
    } 

    public Map<Integer, String> getNeighbours()
    {
        return neighbours;
    }

    public boolean alreadyStreaming(String stream){
        if(this.streamNeighours.containsKey(stream)) 
            return true;
        
        return false;
    }

    public void addStreamingFlux(String streamName, int neighbour){

        if(this.streamNeighours.containsKey(streamName)){
            this.streamNeighours.get(streamName).add(neighbour);
        }else{
            List<Integer> neighbours = new ArrayList<>();
            neighbours.add(neighbour);
            this.streamNeighours.put(streamName, neighbours);
        }
    }

    public List<String> getNeighbourIpsStream(String streamName){
        List<Integer> neighbours = this.streamNeighours.get(streamName);
        return this.getNeighboursIps(neighbours);
    }


    public void run()
    {
        try {
            if (!this.isBoostrapper())
            {
                boolean success = this.messageBootstrapper();
            }

            // Launch tcp worker
            Thread tcp = new Thread(new TCP_Worker(this));
            tcp.start();
            // Launch udp worker
            Thread udp = new Thread(new ONodeHandlerUDP(this));
            udp.start();

            // join threads
            tcp.join();
            udp.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopStreaming(String streamName) {
        this.streamNeighours.remove(streamName);
    }

    public boolean isStreaming(String streamName) {
        return this.streamNeighours.containsKey(streamName);
    }

    public BootstrapperHandler getBootstrapperHandler()
    {
        return this.bootstrapperHandler;
    }

    public boolean isBoostrapper()
    {
        return this.bootstrapperHandler != null;
    }

    public static void main(String args[]){
        List<String> argsL = new ArrayList<>();
        boolean debugMode = false;
        boolean bootstrapMode = !args[0].matches("[0-9]+.[0-9]+.[0-9]+.[0-9]+");

        for (int i=0 ; i<args.length ; i++)
        {
            String arg = args[i];
            if (arg.equals("-g"))
                debugMode = true;
            else
                argsL.add(arg);
        }

        args = argsL.toArray(new String[0]);

        ONode onode;
        if (bootstrapMode)
        {
            onode = new ONode(Integer.parseInt(args[0]), args[1], debugMode);
        }
        else
        {
            onode = new ONode(args[0], debugMode);
        }
        onode.run();
    }
}

class TCP_Worker implements Runnable
{
    private ServerSocket ss;
    private ONode oNode;
    
    public TCP_Worker(ONode node)
    {
        this.oNode = node;
        
        try 
        {
            this.ss = new ServerSocket(Util.PORT);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();    
        }
    }
    
    @Override
    public void run() 
    {
        try
        {
            this.oNode.log(new LogEntry("Now Listening to TCP requests"));
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;
                String address = s.getInetAddress().getHostAddress();
                
                switch(p.type)
                {   
                    case 2: // New stream request from a client
                        this.oNode.log(new LogEntry("New streaming request!"));
                        t = new Thread(new HandleStreamingRequest(this.oNode, p, c));
                        t.start();
                        break;
                    case 4: // Message to bootstrapper
                        if (this.oNode.isBoostrapper())
                        {
                            this.oNode.log(new LogEntry("Received get neighbours message from " + address));
                            t = new Thread(new BootsrapperWorker(this.oNode, this.oNode.getBootstrapperHandler(), c, address));
                            t.start();
                        }
                        else
                        {
                            c.send(8, "Not bootstrapper msg".getBytes());
                            c.stopConnection();
                        }
                        break;
                    case 5: // Flood Message 
                        this.oNode.log(new LogEntry("Received flood message from " + address));
                        t = new Thread(new NormalFloodWorker(this.oNode, p));    
                        t.start();
                        break;
                    case 7: // Liveness check message
                        //this.oNode.log(new LogEntry("Received liveness check from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new LivenessCheckWorker(this.oNode, c, p));
                        t.start();
                        break;
                    default:
                        this.oNode.log(new LogEntry("Packet type < " + p.type + " > not recognized. Message ignored!"));
                        c.stopConnection();
                        break;
                }
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

