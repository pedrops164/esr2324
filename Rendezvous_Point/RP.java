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
                        t = new Thread(new RPWorker1(c, p, this));
                        t.start();
                        break;
                    case 2: // Video stream request
                        this.logger.log(new LogEntry("Received video stream request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new RPWorker2(c, p, this));
                        t.start();
                        break;
                    case 3: // Client requests the available streams
                        this.logger.log(new LogEntry("Received available stream request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new RPWorker3(c, p, this, s.getInetAddress().getHostAddress()));
                        t.start();
                        break;
                    case 4: // Server starts streaming
                        this.logger.log(new LogEntry("Received stream from server " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new RPWorker4(c, p, this));
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

// Responsible to handle new requests from new streams of servers
class RPWorker1 implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;

    public RPWorker1(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        ServerStreams sstreams = ServerStreams.deserialize(in);
        rp.addServerStreams(sstreams.getID(), sstreams.getIP(), sstreams.getStreams());

        // End TCP connection
        this.connection.stopConnection();
    }    
}

// Responsible to handle new video stream requests
class RPWorker2 implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private DatagramSocket ds;
    private Packet receivedPacket;
    private int clientId;

    public RPWorker2(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
        try {
            // open a socket for receiving UDP packets on RP's port
            this.ds = new DatagramSocket(RP.RP_PORT);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void requestStreamToServer(StreamRequest sr){
        try{
            String serverIP = this.rp.getServer(sr.getStreamName());
            Socket s = new Socket(serverIP, Server.SERVER_PORT);
            TCPConnection c = new TCPConnection(s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            sr.serialize(out);
            out.flush();
            byte [] data = baos.toByteArray();
            c.send(2, data); // Send the video stream request to the Server
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void receiveUDPpackets() {
        try {
            // set the buffer size
            int buffersize = 15000;
            // create the buffer to receive the packets
            byte[] receiveData = new byte[buffersize];
    
            this.rp.log(new LogEntry("Listening on UDP in Port " + RP.RP_PORT));
            // Create the packet which will receive the data
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    
            while(true) {
                // Receive the packet
                this.ds.receive(receivePacket);
                // Get the pest path to the client
                Path bestPath = rp.paths.get(this.clientId);
                // get the id of the next node in the path to the client
                int nextNodeId = bestPath.nextNode(this.rp.getId());
                this.rp.log(new LogEntry("Received UDP packet"));

                // Now Send UDP packet to the next Overlay Node in the path of the client

                // get the bytes from the UDP packet, and convert them into UDPDatagram
                byte[] receivedBytes = receivePacket.getData();
                ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
                DataInputStream in = new DataInputStream(bais);
                // build the UDPDatagram from the received bytes (deserialize the bytes)
                UDPDatagram receivedPacket = UDPDatagram.deserialize(in);
                // build the FramePacket to send to the client
                FramePacket fp = new FramePacket(bestPath, receivedPacket);

                // get the bytes of the FramePacket
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                fp.serialize(out);
                out.flush();
                byte[] fpBytes = baos.toByteArray();

                // Get neighbor ip and port (his id is nextNodeId), and send the udp packet to him
                String neighbourIp = this.rp.getNeighbourIp(nextNodeId);
                // Get the UDP packet to send with the bytes from the frame packet
                DatagramPacket udpFramePacket = new DatagramPacket(fpBytes, fpBytes.length, InetAddress.getByName(neighbourIp), ONode.ONODE_PORT);
                // Send the DatagramPacket through the UDP socket
                this.ds.send(udpFramePacket);
                this.rp.log(new LogEntry("Sent UDP packet"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.ds.close();
        }
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        StreamRequest sr = StreamRequest.deserialize(in);
        this.clientId = sr.getClientID();

        try 
        {
            this.rp.log(new LogEntry("A client wants the stream: " + sr.getStreamName() + "!"));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        // Now we have to request to a server to stream this video
        this.requestStreamToServer(sr);

        this.receiveUDPpackets();

        // Now we receive the UDP video stream

        this.connection.stopConnection();

    }    
}

// Responsible to answer the client with the available streams
class RPWorker3 implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;
    private String clientIP;

    public RPWorker3(TCPConnection c, Packet p, RP rp, String clientIP){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
        this.clientIP = clientIP;
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        List<String> streams = rp.getAvailableStreams();

        // Answer
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(streams.size());
            for(String stream : streams){
                out.writeUTF(stream);
            }
            out.flush();
            this.connection.send(3, baos.toByteArray());
            this.rp.log(new LogEntry("Sent available streams to client " + this.clientIP));
        }catch(Exception e){
            e.printStackTrace();
        }

        // End TCP connection
        this.connection.stopConnection();
    }    
}

class RPWorker4 implements Runnable {
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;
    
    public RPWorker4(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
    }

    public void run() {
        byte[] data = this.receivedPacket.data;
        String received = new String(data);

        System.out.println("Received video packet: " + received);
        while(true) {
            try {
                Packet packet = this.connection.receive();
                data = packet.data;
                received = new String(data);
                System.out.println("Received video packet: " + received);
            } catch(EOFException e) {
                // the server closed the TCP connection on his end!!
                System.out.println("The streaming ended!");
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        this.connection.stopConnection();
    }
}