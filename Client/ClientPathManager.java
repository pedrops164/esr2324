package Client;

import java.lang.Runnable;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import Common.LogEntry;
import Common.Path;
import Common.PathNode;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Util;

class ClientPathManager implements Runnable {
    private boolean running;
    private Client client;
    private Map<String, Path> streamPaths;
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
        this.streamPaths = new HashMap<>();
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

    public void setStreamPath(String streamName, Path path) {
        /*
         * Associates the stream with name 'streamName' to the path received as argument
         * This means that this stream is being received through this path
         */
        this.streamPaths.put(streamName, path);
    }

    public Path getStreamPath(String streamName) {
        /*
         * Returns the path from where the stream with name 'streamName' is being streamed
         */
        return this.streamPaths.get(streamName);
    }

    public void removeStreamPath(String streamName) {
        /*
         * Removes the path associated with this stream
         */
        this.streamPaths.remove(streamName);
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

                /*
                 * Iterate through all paths being streamed, and do a liveness check
                 * If the liveness check fails for any path, we get the best path, stop the stream through the
                 * corrupted path, and request stream through the new best path
                 */
                for (Map.Entry<String,Path> entry: this.streamPaths.entrySet()) {
                    String streamName = entry.getKey();
                    Path path = entry.getValue();
                    if (path != null) {
                        boolean alive = this.doLivenessCheck(path);
                        if (!alive) {
                            //this.client.log(new LogEntry("Path " + path.toString() + " is not alive, removing from routing tree."));
                            this.client.removePath(path);

                            // since the path is corrupted, we stop the stream through this path
                            // stopStream is false because we want to continue the stream through another path
                            this.client.requestStopStreaming(streamName, false);
                            
                            // we request the stream through the best new path
                            // If there are no paths, internally there will be a flood and the best path will be set
                            // We set the fixPath flag to true
                            this.client.requestStreaming(streamName, true);
    
                        }
                    }
                }

                //if (paths.size() == 0)
                //{
                //    client.log(new LogEntry("There are no previous calculated paths, a flood will occur"));
                //    client.flood();
                //    start = LocalDateTime.now();
                //}
                //else if (i == paths.size())
                //{
                //    client.log(new LogEntry("The previous calculated paths are not available, a flood will occur"));
                //    client.flood();
                //    start = LocalDateTime.now();
                //}

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
