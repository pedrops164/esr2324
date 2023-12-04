package Overlay_Node;

import java.io.EOFException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

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
    
    @Override
    public void run() {
        while (true)
        {
            List<Integer> changedNodes = this.bootstrapperHandler.parseChanges(AUTOMATIC_VERIFY_TIME);
        
            for (int id : changedNodes)
            {
                List<String> ips = this.bootstrapperHandler.getIPsfromID(id);

                for (String ip : ips)
                {
                    try {
                        Socket socket = new Socket(ip, Util.PORT);
                        TCPConnection tcpConnection = new TCPConnection(socket);

                        tcpConnection.send(8, "changedInfo".getBytes());

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

                            tcpConnection.send(8, serFromID);
                            tcpConnection.send(8, serFromIPs);
                            tcpConnection.send(8, serRPIDs);
                            tcpConnection.send(8, serNeighbours);

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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }    
        }    
    }
    
}
