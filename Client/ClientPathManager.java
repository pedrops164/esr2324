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

class ClientPathWorker implements Runnable {
    private Client client;
    /**
     * Interval in seconds between path verification
     */
    private static long verificationInterval = 512; 

    /**
     * Inteval in seconds between floods
     */
    private static long floodInterval = 2048;

    public ClientPathManager(Client client)
    {
        this.client = client;
    }

    
    public void run()
    {
        try 
        {
            LocalDateTime start = LocalDateTime.now();
            while (true)
            {
                Thread.sleep(verificationInterval * 1000);
                
                for (Path p : client.getOrderedPaths())
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
                            this.client.log(new LogEntry("Sent packet: " + packet + " to " + neighbour));

                            packet = c.receive();
                            this.client.log(new LogEntry("Received packet " + packet + " from " + neighbour));
                        }
                        catch (Exception e)
                        {
                            this.client.log(new LogEntry("Neighbour " + neighbour + " is no longer active. Ignoring path."));
                            this.client.removePath(p);
                            break;
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
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
}
