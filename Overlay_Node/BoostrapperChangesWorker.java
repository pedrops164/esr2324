package Overlay_Node;

import java.io.EOFException;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Common.LogEntry;
import Common.TCPConnection;
import Common.Util;
import Common.TCPConnection.Packet;

public class BoostrapperChangesWorker implements Runnable{
    
    private ONode node;
    private BootstrapperHandler bootstrapperHandler;
    private static long AUTOMATIC_VERIFY_TIME = 10000; 

    public BoostrapperChangesWorker(ONode node, BootstrapperHandler bootstrapperHandler)
    {
        this.node = node;
        this.bootstrapperHandler = bootstrapperHandler;
    }
    
    private void messageRemoved(Set<String> removedNodes)
    {
        for (String ip : removedNodes)
        {
            try {
                Socket socket = new Socket(ip, Util.PORT);
                TCPConnection tcpConnection = new TCPConnection(socket);

                boolean acknowledged = false;
                do
                {
                    tcpConnection.send(4, "REMOVED".getBytes());

                    try 
                    {
                        Packet responsePacket = tcpConnection.receive();
                        String response = new String(responsePacket.data);
                        if (response.equals("OK"))
                        {
                            acknowledged = true;
                            this.node.log(new LogEntry("Bootstrapper : Successfully sent disconnect message to " + ip));
                            break;
                        }
                    } 
                    catch (EOFException e) 
                    {
                        e.printStackTrace();
                    }
                }while(!acknowledged);

                tcpConnection.stopConnection();
            } catch (Exception e) {
                e.printStackTrace();
                this.bootstrapperHandler.removeConnected(ip);
            }
        }
    }

    private void messageChanged(Set<Integer> changedNodes)
    {
        for (int id : changedNodes)
        {
            List<String> ips = this.bootstrapperHandler.getIPsfromID(id);

            for (String ip : ips)
            {
                try {
                    Socket socket = new Socket(ip, Util.PORT);
                    TCPConnection tcpConnection = new TCPConnection(socket);

                    tcpConnection.send(4, "CHANGED".getBytes());

                    Packet p = tcpConnection.receive();
                    if (new String(p.data).equals("OK"))
                    {
                        List<String> newIPs = this.bootstrapperHandler.getIPsfromID(id);

                        List<String> newRPIPs = this.bootstrapperHandler.getRPIPs();

                        Map<Integer,String> newNeighbours = this.bootstrapperHandler.getNeighboursFromID(id);

                        byte[]  serFromID = Util.serializeInt(id),
                                serFromIPs = Util.serializeObject(newIPs),
                                serRPIDs = Util.serializeObject(newRPIPs),
                                serNeighbours = Util.serializeObject(newNeighbours);

                        tcpConnection.send(4, serFromID);
                        tcpConnection.send(4, serFromIPs);
                        tcpConnection.send(4, serRPIDs);
                        tcpConnection.send(4, serNeighbours);

                        try 
                        {
                            p = tcpConnection.receive();
                            String response = new String(p.data);
                            if (response.equals("OK"))
                            {
                                this.node.log(new LogEntry("Bootstrapper : Successfully sent new neighbour information to " + ip));
                                break;
                            }
                            else
                            {
                                this.node.log(new LogEntry("Bootstrapper : Error sending new neighbour information to " + ip));
                            }
                        } 
                        catch (EOFException e) 
                        {
                            e.printStackTrace();
                        }
                    }
                    tcpConnection.stopConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                    this.bootstrapperHandler.removeConnected(id);
                }
            }
        }   
    }

    @Override
    public void run() {
        while (true)
        {
            Set<String> removedNodesIPs = new HashSet<>();
            Set<Integer> changedNodes = this.bootstrapperHandler.parseChanges(AUTOMATIC_VERIFY_TIME, removedNodesIPs);

            this.messageRemoved(removedNodesIPs);
            this.messageChanged(changedNodes);
        }    
    }
    
}
