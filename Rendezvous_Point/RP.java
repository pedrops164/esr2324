package Rendezvous_Point;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.Node;
import Overlay_Node.ONode;
import Common.Path;
import Common.RTPpacket;
import Common.FramePacket;
import Common.UDPDatagram;
import Common.ServerStreams;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.VideoMetadata;
import Server.Server;

import java.awt.event.*;

import javax.swing.ImageIcon;
import javax.swing.Timer;

import java.io.*;
import java.net.*;
import java.util.*;

public class RP extends Node{
    private ServerSocket ss;
    private DatagramSocket ds;
    public static int RP_PORT = 333;
    private byte[] udpBuffer;
    
    // Map that associates each server id to it's available streams
    private Map<String, List<Integer>> streamServers;
    private Map<Integer, String> servers; // serverID to serverIP
    public Map<Integer, Path> paths; // maps clients to their respective paths (paths from RP to each client)
    private int streamCounter;

    public RP(String args[], NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.streamServers = new HashMap<>();
        this.servers = new HashMap<>();
        this.paths = new HashMap<>();

        try{
            this.ss = new ServerSocket(RP_PORT); // socket that receives TCP packets
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Listens to new requests sent to the RP
    public void listen(){
        try{
            this.logger.log(new LogEntry("Now Listening to TCP requests"));
            while(true){
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    case 1: // New available stream in a server
                        this.logger.log(new LogEntry("Received available streams warning from server " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new HandleServerStreams(c, p, this));
                        t.start();
                        break;
                    case 2: // Video stream request
                        this.logger.log(new LogEntry("Received video stream request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new HandleStreamRequests(c, p, this));
                        t.start();
                        break;
                    case 3: // Client requests the available streams
                        this.logger.log(new LogEntry("Received available stream request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new HandleNotifyStreams(c, p, this, s.getInetAddress().getHostAddress()));
                        t.start();
                        break;
                    case 5: // Client Flood Message
                        this.logger.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new RPFloodWorker(this, p));
                        t.start();
                        break;
                    default:
                        this.logger.log(new LogEntry("Packet type not recognized. Message ignored! Type: " + p.type ));
                        c.stopConnection();
                        break;
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    Associates client id to its path
    */
    public void addPathToClient(int clientId, Path path) {
        this.paths.put(clientId, path);
    }

    public synchronized void addServerStreams(int serverID, String serverIP, List<String> streams){

        try 
        {
            this.logger.log(new LogEntry("Adding available streams from server " + serverIP));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }

        for (String stream : streams)
        {
            List<Integer> servers;
            if (this.streamServers.containsKey(stream))
                servers = this.streamServers.get(stream);
            else
                servers = new ArrayList<>();
            servers.add(serverID);
            this.streamServers.put(stream, servers);
            this.servers.put(serverID, serverIP);
        }
    }

    // Get the server that has a certain stream 
    // This has to be updated!!! We have to check which is the best server to stream!
    // For now we only return the first server.
    public synchronized String getServer(String streamName){
        int id = this.streamServers.get(streamName).get(0);
        return this.servers.get(id);
    }

    public synchronized List<String> getAvailableStreams(){
        List<String> streams = new ArrayList<>();
        for(Map.Entry<String, List<Integer>> entry : this.streamServers.entrySet()){
            streams.add(entry.getKey());
        }
        return streams;
    }

    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        boolean debugMode = Arrays.stream(args).anyMatch(s -> s.equals("-g"));
        RP rp = new RP(args, nr, debugMode);
        rp.listen(); 
    }

}