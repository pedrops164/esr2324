package Common;

import java.io.IOException;
import java.net.Socket;

import Common.TCPConnection.Packet;

public class LivenessCheckWorker implements Runnable {
    private Node node;
    private TCPConnection c;
    private Packet packet;

    public LivenessCheckWorker(Node node, TCPConnection c, Packet packet)
    {
        this.node = node;
        this.c = c;
        this.packet = packet;
    }

    public void sendResponse(boolean alive) throws IOException
    {
        byte[] msg = new byte[1];
        msg[0] = (byte) ((alive) ?1 :0);
        this.c.send(7, msg);
        this.c.stopConnection();
        this.node.log(new LogEntry("Sent liveness check answer saying path is " + ((!alive) ?"not " :"") + "alive back."));
    }

    @Override
    public void run() 
    {
        try 
        {
            Path path = Path.deserialize(this.packet.data);
            byte[] serializedPath = path.serialize();
            PathNode nextNeighbour = path.getNext(this.node.getId()); 
            String nextNeighbourIP = nextNeighbour.getNodeIPAddress().toString();
            boolean alive = false;

            try
            {
                Socket socket = new Socket(nextNeighbour.getNodeIPAddress().toString(), 333);
                TCPConnection connection2 = new TCPConnection(socket);
                connection2.send(7, serializedPath);

                Packet responsePacket = connection2.receive();
                alive = Boolean.parseBoolean(new String(responsePacket.data));
                this.node.log(new LogEntry("Received liveness check confirmation from " + nextNeighbourIP  + ". Path is " + ((!alive) ?"not " :"") + "alive"));
            }
            catch (Exception e)
            {
                this.node.log(new LogEntry("Neighbour " + nextNeighbourIP + " is no longer active. Sending confirmation to client."));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
