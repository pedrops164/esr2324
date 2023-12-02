package Common;

import java.io.IOException;
import java.net.Socket;
import java.util.Map.Entry;
import Common.TCPConnection.Packet;

public class NormalFloodWorker implements Runnable 
{
    private Node node;
    private Path path;

    public NormalFloodWorker (Node node, Packet p)
    {
        this.node = node;
        this.path = Path.deserialize(p.data);
        this.path.addNode(new PathNode(this.node.getId(), 333, node.getIp()));
    }

    public void run ()
    {
        byte[] serializedPath = this.path.serialize();
        for (Entry<Integer, String> neighbour : node.getNeighbours().entrySet())
        {
            if (path.inPath(neighbour.getKey()) || neighbour.getKey() == this.node.getId())
                continue;

            try
            {
                Socket s = new Socket(neighbour.getValue(), 333);
                TCPConnection c = new TCPConnection(s);
                Packet p = new Packet(5, serializedPath);
                c.send(p);
                c.stopConnection();
                this.node.log(new LogEntry("Sent flood message to " + neighbour + ":" + 333));
            }
            catch (Exception eFromSocket)
            {
                this.node.log(new LogEntry("Error sending flood message to " + neighbour + ". Retrying later"));
            }
        }
    }
}