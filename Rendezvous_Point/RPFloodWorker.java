package Rendezvous_Point;

import java.net.Socket;

import Common.LogEntry;
import Common.Path;
import Common.PathNode;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

public class RPFloodWorker implements Runnable 
{
    private RP rp;
    private Path path;

    public RPFloodWorker (RP rp, Packet p)
    {
        this.rp = rp;
        this.path = Path.deserialize(p.data);
    }

    public void run ()
    {
        PathNode pathNode = new PathNode(this.rp.getId(), 333, rp.getIp());
        if (!this.path.inPath(pathNode))
        {
            this.path.addNode(pathNode);
            byte[] serializedPath = this.path.serialize();
            try {
                PathNode client = path.getClient();
                this.rp.addPathToClient(client.getNodeId(), this.path);
                Socket s = new Socket(client.getNodeIPAddress().toString(), 333);
                TCPConnection c = new TCPConnection(s);
                c.send(new Packet(6, serializedPath));
                this.rp.log(new LogEntry("Sent flood response to client: " + client.getNodeIPAddress().toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}