package Client;

import java.lang.Runnable;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import Common.LogEntry;
import Common.Path;
import Common.PathNode;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Util;

class ClientPathManager implements Runnable {
    private boolean running;
    private Client client;
    /**
     * Interval in millis between path verification
     */
    private static long verificationInterval = 250; 

    /**
     * Inteval in millis between floods
     */
    private static long floodInterval = 5000;

    public ClientPathManager(Client client)
    {
        this.client = client;
    }

    public boolean doLivenessCheck(Path path)
    {
        try 
        {
            int clientID = client.getId();
            PathNode pn = path.getNext(clientID);
            String neighbour = pn.getNodeIPAddress().toString();

            byte[] serializedPath = path.serialize();
            boolean alive = false;
            try 
            {
                // falar com o nó
                Socket s = new Socket(neighbour, Util.PORT);
                TCPConnection c = new TCPConnection(s);
                Packet packet = new Packet(7, serializedPath);
                c.send(packet);
                //this.client.log(new LogEntry("Sent liveness check to " + neighbour));

                packet = c.receive();
                alive = packet.data[0] != 0; 
                //this.client.log(new LogEntry("Received liveness check confirmation from " + neighbour + ". Path is " + ((!alive) ?"not " :"") + "alive"));
                c.stopConnection();
            }
            catch (Exception e)
            {
                //this.client.log(new LogEntry("Neighbour " + neighbour + " is no longer active. Ignoring path."));
            }

            return alive;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public void run()
    {
        try 
        {
            this.running = true;
            this.client.log(new LogEntry("Path Manager started.."));
            LocalDateTime start = LocalDateTime.now();

            while (this.running)
            {
                List<Path> paths = this.client.getOrderedPaths();
                
                int i;
                for (i=0 ; i<paths.size() ; i++)
                {
                    Path p = paths.get(i);
                    boolean alive = this.doLivenessCheck(p);
                    if (!alive)
                    {
                        //this.client.log(new LogEntry("Path " + p.toString() + " is not alive, removing from routing tree."));
                        this.client.removePath(p);
                    }
                    else
                        break;
                }

                if (paths.size() == 0)
                {
                    client.log(new LogEntry("There are no previous calculated paths, a flood will occur"));
                    client.flood();
                    start = LocalDateTime.now();
                }
                else if (i == paths.size())
                {
                    client.log(new LogEntry("The previous calculated paths are not available, a flood will occur"));
                    client.flood();
                    start = LocalDateTime.now();
                }

                if (ChronoUnit.MILLIS.between(start, LocalDateTime.now()) >= floodInterval)
                {
                    client.log(new LogEntry(floodInterval + " milliseconds have passed, a flood will occur"));
                    client.flood();
                    start = LocalDateTime.now();
                }
                Thread.sleep(verificationInterval);
            }    
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    public void turnOff()
    {
        this.running = false;
    }
}
