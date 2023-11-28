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

import Rendezvous_Point.RPHandlerTCP;
import Rendezvous_Point.RPHandlerUDP;

import java.awt.event.*;

import javax.swing.ImageIcon;
import javax.swing.Timer;

import java.io.*;
import java.net.*;
import java.util.*;

public class RP extends Node{
    public static int RP_PORT = 333;
    
    private Map<String, List<Integer>> streamServers; // stream name to list of available servers
    private Map<Integer, String> servers; // serverID to serverIP
    public Map<Integer, Path> paths; // maps clients to their respective paths (paths from RP to each client)
    private Map<String, List<Integer>> streamsClients; // stream name to list of clients that want it
    private int streamCounter;

    public RP(String args[], NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.streamServers = new HashMap<>();
        this.servers = new HashMap<>();
        this.paths = new HashMap<>();
        this.streamsClients = new HashMap<>();
    }

    public void run() {
        try {
            // Launch tcp worker
            Thread tcp = new Thread(new RPHandlerTCP(this));
            tcp.start();
            // Launch udp worker
            Thread udp = new Thread(new RPHandlerUDP(this));
            udp.start();

            // join threads
            tcp.join();
            udp.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Associates client id to its path
    */
    public void addPathToClient(int clientId, Path path) {
        this.paths.put(clientId, path);
    }

    public void addClientToStream(String streamName, int clientId) {
        // Adds the client to the mapping between streams and the clients watching them
        if (!this.streamsClients.containsKey(streamName)) {
            this.streamsClients.put(streamName, new ArrayList<Integer>());
        }
        this.streamsClients.get(streamName).add(clientId);
    }

    public List<Integer> getStreamClients(String streamName) {
        List<Integer> clients = this.streamsClients.get(streamName);
        if (clients != null) {
            return new ArrayList<Integer>(clients);
        }
        return clients;
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
                servers = new ArrayList<Integer>();
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
        List<String> streams = new ArrayList<String>();
        for(Map.Entry<String, List<Integer>> entry : this.streamServers.entrySet()){
            streams.add(entry.getKey());
        }
        return streams;
    }

    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        boolean debugMode = Arrays.stream(args).anyMatch(s -> s.equals("-g"));
        RP rp = new RP(args, nr, debugMode);
        rp.run(); 
    }

}