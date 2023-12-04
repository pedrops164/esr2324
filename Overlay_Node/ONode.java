package Overlay_Node;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.LivenessCheckWorker;
import Common.Node;
import Common.PathNode;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.UDPDatagram;
import Common.VideoMetadata;
import Common.TCPConnection.Packet;
import Common.NormalFloodWorker;

import java.net.*;
import java.util.*;

public class ONode extends Node {
    public static int ONODE_PORT = 333;
    private Map<String, List<Integer>> streamNeighours;

    public ONode(int id, NeighbourReader nr, boolean debugMode)
    {
        super(id, nr, debugMode);
        this.streamNeighours = new HashMap<>();
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

    public static void main(String args[]){
        int id = Integer.parseInt(args[0]);
        NeighbourReader nr = new NeighbourReader(id, args[1]);
        boolean debugMode = Arrays.stream(args).anyMatch(s -> s.equals("-g"));
        ONode onode = new ONode(id, nr, debugMode);
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
            this.ss = new ServerSocket(ONode.ONODE_PORT);
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
                    case 5: // Flood Message 
                        this.oNode.log(new LogEntry("Received flood message from " + address));
                        t = new Thread(new NormalFloodWorker(this.oNode, p));    
                        t.start();
                        break;
                    case 7: // ALIVE? message
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

