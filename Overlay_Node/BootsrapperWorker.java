package Overlay_Node;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import Common.LogEntry;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Utility;

public class BootsrapperWorker implements Runnable {

    private ONode node;
    private BootstrapperHandler bootstrapperHandler;
    private TCPConnection connection;
    private String fromIp;

    public BootsrapperWorker(ONode node, BootstrapperHandler bootstrapperHandler, TCPConnection connection, String fromIp)
    {
        this.node = node;
        this.bootstrapperHandler = bootstrapperHandler;
        this.connection = connection;
        this.fromIp = fromIp;
    }

    @Override
    public void run() {
        // Verificar existencia
        int fromID = this.bootstrapperHandler.getIdFromIP(fromIp);
        if (fromID == -1)
            return;  //TODO acabar isto

        List<String> RPIPs = this.bootstrapperHandler.getRPIPs();

        Map<Integer,String> neighbours = this.bootstrapperHandler.getNeighboursFromID(fromID);

        byte[]  serFromID = Utility.serializeInt(fromID),
                serRPIDs = Utility.serializeObject(RPIPs),
                serNeighbours = Utility.serializeObject(neighbours);

        this.connection.send(8, serFromID);
        this.connection.send(8, serRPIDs);
        this.connection.send(8, serNeighbours);

        try 
        {
            Packet p = this.connection.receive();
            String response = new String(p.data);
            if (response.equals("OK"))
            {
                this.node.log(new LogEntry("Bootstrapper : Successfully sent neighbour information to " + fromIp));
            }
            else
            {
                this.node.log(new LogEntry("Bootstrapper : Error sending neighbour information to " + fromIp));
            }
        } 
        catch (EOFException e) 
        {
            e.printStackTrace();
        }
    }
    
}
