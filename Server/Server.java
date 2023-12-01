package Server;

import Common.LivenessCheckWorker;
import Common.LogEntry;
import Common.NeighbourReader;
import Common.Node;
import Common.NormalFloodWorker;
import Common.ServerStreams;
import Common.StreamRequest;
import Common.UDPDatagram;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Rendezvous_Point.RP;
import Common.Video;
import Common.VideoMetadata;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Node {
    private ServerSocket ss;
    public static int SERVER_PORT = 1234;

    private List<String> streams;

    public Server(String []args, NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.streams = new ArrayList<>();

        try{
            this.ss = new ServerSocket(SERVER_PORT);
        }catch(Exception e){
            e.printStackTrace();
        }

        try 
        {
            this.logger.log(new LogEntry("Getting availabe Streams"));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }

        File path = new File(args[2]);
        File [] listOfFiles = path.listFiles();
        for(File f : listOfFiles){
            if(f.isFile()){
                this.streams.add(f.getName());
            }
        }
    }

    // Notify the RP about the available streams in this Server
    public boolean notifyStreamsRP(){
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 1000; // Delay between retries in milliseconds (1 second)
        int retryCount = 0;
        do {
            try{    
                // Send request
                ServerStreams sstreams = new ServerStreams(this.streams, this.id, this.ip);
                Socket s = new Socket(this.RPIP, RP.RP_PORT);
                TCPConnection c = new TCPConnection(s);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                sstreams.serialize(out);
                out.flush();
                byte [] data = baos.toByteArray();
                c.send(1, data); // Send the request to the RP
                this.logger.log(new LogEntry("Notifying Rendezvous Point about the available streams"));
                
                return true;
            }catch(ConnectException e) {
                try 
                {
                    this.logger.log(new LogEntry("ConnectException caught!!"));
                    retryCount++;
                    this.logger.log(new LogEntry("Unable to connect to RP. Retrying in " + retryInterval / 1000 + " seconds. Attempt " + retryCount));
                    try {
                        Thread.sleep(retryInterval); // Wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.out.println("Thread interrupted during retry wait.");
                        break;
                    }
                }
                catch (IOException e2)
                {
                    e.printStackTrace();
                }
            }catch(Exception e){
                e.printStackTrace();
                break;
            }
        } while (retryCount < maxRetries);
        
        try 
        {
            this.logger.log(new LogEntry("Server: Couldn't establish connection with RP."));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }

        return false;
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
                    case 2: // New video stream request
                        this.logger.log(new LogEntry("Received Video Stream Request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new ServerWorker1(c, p, this.RPIP, this));
                        t.start();
                        break;
                    case 5: // Flood message
                        this.logger.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new NormalFloodWorker(this, p));
                        t.start();
                        break;
                    case 7: // LIVENESS CHECK message
                        this.log(new LogEntry("Received liveness check from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new LivenessCheckWorker(this, c, p));
                        t.start();
                        break;
                    default:
                        this.logger.log(new LogEntry("Packet type not recognized. Message ignored!"));
                        c.stopConnection();
                        break;
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void availableStreams(){
        System.out.println("Available Streams:");
        for(String stream : this.streams){
            System.out.println(stream);
        }
    }

    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        boolean debugMode = Arrays.stream(args).anyMatch(s -> s.equals("-g"));

        Server server = new Server(args, nr, debugMode);

        // Tell the available streams to the RP 
        if (server.notifyStreamsRP()) {
            // notifyStreamsRP returns true if it went successful
            server.listen(); // Listen to new TCP requests
        }
    }
}

// Responsible to handle new resquests of video streaming!
class ServerWorker1 implements Runnable{
    private TCPConnection connection;
    private Packet receivedPacket;
    private String RPIP;
    private DatagramSocket ds;
    private Node node;

    public ServerWorker1(TCPConnection connection, Packet p, String RPIP, Node node){
        this.connection = connection;
        this.receivedPacket = p;
        this.RPIP = RPIP;
        this.node = node;
        try {
            this.ds = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        StreamRequest sr = StreamRequest.deserialize(data);

        //Deserialize the Path

        String videoPath = sr.getStreamName();

        // get video attributes....
        int videoLength = 100; // number of frames
        int frame_period = 75; // time between frames in ms.
        int video_extension = 26; //26 is Mjpeg type

        VideoMetadata vmd = new VideoMetadata(frame_period);
        // Convert VideoMetadata to bytes (serialize) and send the packet to RP (type 6 represents video metadata)
        Packet packetMetadata = new Packet(6, vmd.serialize());
        this.connection.send(packetMetadata);

        this.connection.stopConnection();

        // Start the UDP video streaming. (Send directly to the RP)
        try {
            this.node.log(new LogEntry("Sent VideoMetadata packet to RP"));
            this.node.log(new LogEntry("Streaming '" + videoPath + "' through UDP!"));
            Video video = new Video(videoPath);
            for (int frameNumber = 0; frameNumber < videoLength; frameNumber++) {
                // Get the next frame of the video
	            byte[] videoBuffer = video.getNextVideoFrame();
                //Builds a UDPDatagram object containing the frame
	            UDPDatagram udpDatagram = new UDPDatagram(video_extension, frameNumber, frameNumber*frame_period,
                 videoBuffer, videoBuffer.length, videoPath);
                
                // get the bytes of the UDPDatagram
                byte[] packetBytes = udpDatagram.serialize();

	            // Initialize the DatagramPacket and send it over the UDP socket 
	            DatagramPacket senddp = new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName(this.RPIP), RP.RP_PORT);
	            this.ds.send(senddp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
}
