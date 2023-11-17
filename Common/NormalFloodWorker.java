package Common;

import java.net.Socket;
import java.util.Map.Entry;
import Common.TCPConnection.Packet;

public class NormalFloodWorker implements Runnable 
{
    private Node node;
    private Path path;
    private PathNode sender;

    public NormalFloodWorker (Node node, Packet p)
    {
        this.node = node;
        this.path = Path.deserialize(p.data);
        this.sender = this.path.getLast();
        this.path.addNode(new PathNode(this.node.getId(), 333, node.getIp()));
    }

    public void run ()
    {
        byte[] serializedPath = this.path.serialize();
        try 
        {
            for (Entry<Integer, String> neighbour : node.getNeighbours().entrySet())
            {
                if (neighbour.getKey() == sender.getNodeId())
                    continue;
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