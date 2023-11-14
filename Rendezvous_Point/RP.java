package Rendezvous_Point;

import Common.NeighbourReader;
import Common.ServerStreams;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

import java.io.*;
import java.net.*;
import java.util.*;

public class RP{
    private int id;
    private Map<Integer, String> neighbours;
    private ServerSocket ss;
    
    // Map that associates each server id to it's available streams
    private Map<String, List<Integer>> streamServers;
    private Map<Integer, String> servers; // serverID to serverIP
    private int streamCounter;

    public RP(String args[], NeighbourReader nr){
        this.id = Integer.parseInt(args[0]);
        this.neighbours = nr.readNeighbours();
        this.streamServers = new HashMap<>();
        this.servers = new HashMap<>();

        try{
            this.ss = new ServerSocket(333);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Listens to new requests sent to the RP
    public void listen(){
        while(true){
            try{
                System.out.println("RP waiting for new requests!");
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    case 1: // New available stream in a server
                        t = new Thread(new RPWorker1(c, p, this));
                        t.start();
                        break;
                    case 2: // Video stream request
                        t = new Thread(new RPWorker2(c, p, this));
                        t.start();
                        break;
                    case 3: // Client requests the available streams
                        t = new Thread(new RPWorker3(c, p, this));
                        t.start();
                        break;
                    case 4: // Server starts streaming
                        t = new Thread(new RPWorker4(c, p, this));
                        t.start();
                        break;
                    default:
                        System.out.println("Packet type not recognized. Message ignored!");
                        c.stopConnection();
                        break;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public synchronized void addServerStreams(int serverID, String serverIP, List<String> streams){
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
        System.out.println(this.streamServers);
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
        RP rp = new RP(args, nr);
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

        // Answer
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Ok!");
            out.flush();
            this.connection.send(1, baos.toByteArray());
        }catch(Exception e){
            e.printStackTrace();
        }

        // End TCP connection
        this.connection.stopConnection();
    }    
}

// Responsible to handle new video stream requests
class RPWorker2 implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;

    public RPWorker2(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
    }

    public void requestStreamToServer(StreamRequest sr){
        try{
            String serverIP = this.rp.getServer(sr.getStreamName());
            Socket s = new Socket(serverIP, 1234);
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

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        StreamRequest sr = StreamRequest.deserialize(in);

        System.out.println("A client wants me to stream: " + sr.getStreamName() + "!");
        // Now we have to request to a server to stream this video
        this.requestStreamToServer(sr);

        // Now we receive the UDP video stream

        this.connection.stopConnection();

    }    
}

// Responsible to answer the client with the available streams
class RPWorker3 implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;

    public RPWorker3(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
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