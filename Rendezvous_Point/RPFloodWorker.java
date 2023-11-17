package Rendezvous_Point;

import java.net.Socket;
import java.util.Map.Entry;

import Common.Node;
import Common.Path;
import Common.PathNode;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

public class RPFloodWorker implements Runnable 
{
    private Node node;
    private Path path;

    public RPFloodWorker (Node node, Packet p)
    {
        this.node = node;
        this.path = Path.deserialize(p.data);
        this.path.addNode(new PathNode(this.node.getId(), 333, node.getIp()));
    }

    public void run ()
    {
        byte[] serializedPath = this.path.serialize();
        try 
        {
            Socket s = new Socket(path.getClient().getNodeIPAddress().toString(), 333);
            TCPConnection c = new TCPConnection(s);
            c.send(new Packet(6, serializedPath));
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}