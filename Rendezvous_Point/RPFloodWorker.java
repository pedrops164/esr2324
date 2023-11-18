package Rendezvous_Point;

import java.net.Socket;

import Common.LogEntry;
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
    }

    public void run ()
    {
        PathNode pathNode = new PathNode(this.node.getId(), 333, node.getIp());
        if (!this.path.inPath(pathNode))
        {
            this.path.addNode(pathNode);
            byte[] serializedPath = this.path.serialize();
            try 
            {
                PathNode client = path.getClient();
                Socket s = new Socket(client.getNodeIPAddress().toString(), 333);
                TCPConnection c = new TCPConnection(s);
                c.send(new Packet(6, serializedPath));
                this.node.log(new LogEntry("Sent flood response to client: " + client));
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
        }
    }
}