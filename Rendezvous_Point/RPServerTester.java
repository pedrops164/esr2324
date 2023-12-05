package Rendezvous_Point;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import Common.LogEntry;
import Common.ServerStreams;
import Common.TCPConnection;
import Common.Util;

/*
* This class is responsible from time to time to test each server (update the rankings).
* This class requests for each server a ServerStreams reply each 2 seconds.
*/

public class RPServerTester implements Runnable{
    private RP rp;
    private Map<Integer, TCPConnection> serverConnections;
    private boolean running;

    public RPServerTester(RP rp){
        this.rp = rp;

        // Set the sockets to communicate with the servers
        Map<Integer, String> servers = this.rp.getServers();
        this.serverConnections = new HashMap<>();
        for(Map.Entry<Integer,String> entry : servers.entrySet()){
            addServerConnection(entry);
        }
    }

    public void addServerConnection(Map.Entry<Integer, String> server){
        try{
            Socket s = new Socket(server.getValue(), Util.PORT);
            TCPConnection c = new TCPConnection(s);
            this.serverConnections.put(server.getKey(), c);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        this.running = true;
        while(this.running){
            Map<Integer, String> servers = this.rp.getServers();
            try{
                for(Map.Entry<Integer,String> server : servers.entrySet()){
                    this.rp.log(new LogEntry("RP requesting ServerStreams from the " + server.getKey() + " server!"));
                    // If we don't have a connection set with this server we set one
                    if(!this.serverConnections.containsKey(server.getKey())){
                        addServerConnection(server);
                    }

                    // Send request for ServerStreams
                    byte[] msg = "ServerStreams request!".getBytes();
                    TCPConnection c = this.serverConnections.get(server.getKey());
                    c.send(new TCPConnection.Packet(1, msg));

                    // Server Response
                    TCPConnection.Packet receivedPacket = c.receive();
                    byte[] data = receivedPacket.data;
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream in = new DataInputStream(bais);
                    ServerStreams sstreams = ServerStreams.deserialize(in);
                    
                    this.rp.log(new LogEntry("RP received the ServerStreams response and is going to update the server ranking!"));

                    // Rank server connection speed
                    LocalDateTime receivingTimeStamp = LocalDateTime.now();
                    this.rp.rankServer(sstreams, receivingTimeStamp);
                }       
                Thread.sleep(2000);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        this.serverConnections.values().stream().map((c) -> { c.stopConnection(); return c; });
    }

    public void turnOff()
    {
        this.running = false;
    }
}
