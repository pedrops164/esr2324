package Client;

import java.lang.Runnable;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReentrantLock;

import Common.InvalidNodeException;
import Common.LogEntry;
import Common.Path;
import Common.PathNode;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

public class ClientPathManager implements Runnable {
    private Client client;
    private RoutingTree clientRoutingTree;
    private ReentrantLock clientRoutingTreeLock;
    
    /**
     * Interval in seconds between path verification
     */
    private static long verificationInterval = 512; 

    /**
     * Inteval in seconds between floods
     */
    private static long floodInterval = 2048;

    public ClientPathManager(Client client, RoutingTree clientRoutingTree, ReentrantLock clientRoutingTreeLock)
    {
        this.client = client;
        this.clientRoutingTree = clientRoutingTree;
        this.clientRoutingTreeLock = clientRoutingTreeLock;
    }


    public void run()
    {
        try 
        {
            LocalDateTime start = LocalDateTime.now();
            while (true)
            {
                Thread.sleep(verificationInterval * 1000);
                
                try
                {
                    this.clientRoutingTreeLock.lock();
                    for (Path p : clientRoutingTree.getOrderedPaths())
                    {
                        int clientID = client.getId();
                        PathNode pn = p.getNext(clientID);
                        while (true)
                        {
                            String neighbour = pn.getNodeIPAddress().toString();
                            try 
                            {
                                // falar com o nó
                                Socket s = new Socket(neighbour, 334);
                                TCPConnection c = new TCPConnection(s);
                                Packet packet = new Packet(7, "ALIVE?".getBytes());
                                c.send(packet);
    
                                packet = c.receive();                               
                            } 
                            catch (Exception e) 
                            {
                                this.client.log(new LogEntry("Neighbour " + neighbour + " is no longer active. Ignoring path."));
                            }

                            try 
                            {
                                pn = p.getNext(pn.getNodeId());  
                            } 
                            catch (InvalidNodeException e) 
                            {
                                break;
                            }
                        }
                    }

                    if (ChronoUnit.SECONDS.between(start, LocalDateTime.now()) >= floodInterval)
                    {
                        client.log(new LogEntry(floodInterval + " seconds have passed, a flood will occur"));
                        client.flood();
                        start = LocalDateTime.now();
                    }
                }
                finally
                {
                    this.clientRoutingTreeLock.unlock();
                }
            }    
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
