package Overlay_Node;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import Common.NeighbourReader;
import Common.Path;
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
        while(true)
        {
            Socket s = this.ss.accept();
            TCPConnection c = new TCPConnection(s);
            Packet p = c.receive();
            
            switch(p.type)
            {
                case 5: // Flood Message from client
                
            }
        }
    }
}

class FloodWorker implements Runnable 
{
    private ONode node;
    private Path p;

    public FloodWorker (ONode node, Packet p)
    {
        this.node = node;
        this.p = Path.deserialize(p.data);
    }

    public void run ()
    {
        try 
        {
            Socket s = 
        } 
        catch (Exception e) 
        {
            // TODO: handle exception
        }
    }
}