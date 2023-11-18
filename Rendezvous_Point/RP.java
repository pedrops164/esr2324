package Rendezvous_Point;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.Node;
//import Common.RTPpacket;
import Common.ServerStreams;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

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
    private int streamCounter;

    public RP(String args[], NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.streamServers = new HashMap<>();
        this.servers = new HashMap<>();

        try{
            this.ss = new ServerSocket(RP_PORT); // socket that receives TCP packets
            this.ds = new DatagramSocket(RP_PORT); // socket that receives UDP packets
        }catch(Exception e){
            e.printStackTrace();
        }

        Timer udpTimer = new Timer(20, new handlerUDP(this.ds, this));
        udpTimer.setInitialDelay(0);
        udpTimer.setCoalesce(true);
        udpTimer.start();

        udpBuffer = new byte[15000];
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
                    default:
                        this.logger.log(new LogEntry("Packet type not recognized. Message ignored!"));
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

    class handlerUDP implements ActionListener {
        private DatagramSocket ds;
        private Node node;

        public handlerUDP(DatagramSocket ds, Node node) {
            super();
            this.ds = ds;
            this.node = node;
        }

        public void actionPerformed(ActionEvent e) {
            //Construct a DatagramPacket to receive data from the UDP socket
            DatagramPacket rcvdp = new DatagramPacket(udpBuffer, udpBuffer.length);

            try{
	        //     //receive the DP from the socket:
	        //     this.ds.receive(rcvdp);
                this.node.log(new LogEntry("Received UDP request from " + "xxx.xxx.xxx.xxx"));
	        //     //create an RTPpacket object from the DP
	        //     RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());

	        //     //print important header fields of the RTP packet received: 
	        //     //System.out.println("Got RTP packet with SeqNum # "+rtp_packet.getsequencenumber()+" TimeStamp "+rtp_packet.gettimestamp()+" ms, of type "+rtp_packet.getpayloadtype());
	            
            //     //get the payload bitstream from the RTPpacket object
	        //     int payload_length = rtp_packet.getpayload_length();
	        //     byte [] payload = new byte[payload_length];
	        //     rtp_packet.getpayload(payload);
            // } catch (InterruptedIOException iioe){
	        //     System.out.println("Nothing to read");
            } catch (IOException ioe) {
	            System.out.println("Exception caught: "+ioe);
            }
        }
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
            this.rp.log(new LogEntry("Answer sent to server " + sstreams.getIP()));
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