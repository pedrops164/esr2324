package Overlay_Node;

import java.io.EOFException;
import java.util.List;
import java.util.Map;

import Common.LogEntry;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Util;

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
        {
            this.node.log(new LogEntry("Couldn't find Node with ip " + fromIp + " on config file.."));
            return;  //TODO acabar isto
        }

        List<String> fromIPs = this.bootstrapperHandler.getIPsfromID(fromID);

        List<String> RPIPs = this.bootstrapperHandler.getRPIPs();

        Map<Integer,String> neighbours = this.bootstrapperHandler.getNeighboursFromID(fromID);

        byte[]  serFromID = Util.serializeInt(fromID),
                serFromIPs = Util.serializeObject(fromIPs),
                serRPIDs = Util.serializeObject(RPIPs),
                serNeighbours = Util.serializeObject(neighbours);

        this.connection.send(8, serFromID);
        this.connection.send(8, serFromIPs);
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
