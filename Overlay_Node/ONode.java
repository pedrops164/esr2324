package Overlay_Node;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.Node;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.NormalFloodWorker;

public class ONode extends Node {


    public ONode(int id, NeighbourReader nr, boolean debugMode)
    {
        super(id, nr, debugMode);
    }

    public int getId()
    {
        return id;
    } 

    public Map<Integer, String> getNeighbours()
    {
        return neighbours;
    }

    public void run()
    {
        Thread tcp = new Thread(new TCP_Worker(this));
        tcp.start();
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
    private Node node;
    
    public TCP_Worker(Node node)
    {
        this.node = node;
        
        try 
        {
            this.ss = new ServerSocket(333);
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
            this.node.log(new LogEntry("Now Listening to TCP requests"));
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                
                switch(p.type)
                {
                    case 5: // Flood Message 
                        this.node.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        Thread t = new Thread(new NormalFloodWorker(node, p));    
                        t.start();
                    default:
                        this.node.log(new LogEntry("Packet type not recognized. Message ignored!"));
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