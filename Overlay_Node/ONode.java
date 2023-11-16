package Overlay_Node;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Map.Entry;

import Common.NeighbourReader;
import Common.Path;
import Common.PathNode;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

public class ONode {
    private int id;
    private Map<Integer, String> neighbours;

    public ONode(int id, NeighbourReader nr)
    {
        this.id = id;
        this.neighbours = nr.readNeighbours(); 
        
    }

    public int getId()
    {
        return id;
    } 

    public Map<Integer, String> getNeighbours()
    {
        return neighbours;
    }
}

class TCP_Worker implements Runnable
{
    private ServerSocket ss;
    private ONode node;
    
    public TCP_Worker(ONode node)
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
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                
                switch(p.type)
                {
                    case 5: // Flood Message from client
                        Thread t = new Thread(new FloodWorker(node, p, s.getInetAddress().getHostAddress()));    
                        t.start();
                }
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

class FloodWorker implements Runnable 
{
    private ONode node;
    private Path path;

    public FloodWorker (ONode node, Packet p, String ip)
    {
        this.node = node;
        this.path = Path.deserialize(p.data);
        this.path.addNode(new PathNode(this.node.getId(), 333, ip));
    }

    public void run ()
    {
        byte[] serializedPath = this.path.serialize();
        try 
        {
            for (Entry<Integer, String> neighbour : node.getNeighbours().entrySet())
            {
                Socket s = new Socket(neighbour.getValue(), 333);
                TCPConnection c = new TCPConnection(s);
                c.send(new Packet(5, serializedPath));
            }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}