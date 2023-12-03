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
    public static int SERVER_PORT = 333;
    private List<String> streams;
    private String streamsDir;

    public Server(String []args, NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.streams = new ArrayList<>();
        this.streamsDir = args[2];
        
        if(this.streamsDir.charAt(this.streamsDir.length() - 1) != '/'){
            this.streamsDir += "/";
        }

        try{
            this.ss = new ServerSocket(SERVER_PORT);
            this.logger.log(new LogEntry("Getting availabe Streams"));
        } catch(Exception e){
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
                TCPConnection tcpConnection = new TCPConnection(s);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                sstreams.serialize(out);
                out.flush();
                byte [] data = baos.toByteArray();
                tcpConnection.send(1, data); // Send the request to the RP
                this.logger.log(new LogEntry("Notifying Rendezvous Point about the available streams"));
                
                return true;
            }catch(ConnectException e) {
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
            }catch(Exception e){
                e.printStackTrace();
                break;
            }
        } while (retryCount < maxRetries);
        
        this.logger.log(new LogEntry("Server: Couldn't establish connection with RP."));

        return false;
    }   

    // Listens to new requests sent to the RP
    public void listen(){
        try{
            this.logger.log(new LogEntry("Now Listening to TCP requests"));
            while(true){
                Socket socket = this.ss.accept();
                TCPConnection tcpConnection = new TCPConnection(socket);
                Packet p = tcpConnection.receive();
                Thread t;

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    case 2: // New video stream request
                        this.logger.log(new LogEntry("Received Video Stream Request from " + socket.getInetAddress().getHostAddress()));
                        t = new Thread(new ServerWorker1(tcpConnection, p, this.RPIP, this));
                        t.start();
                        break;
                    case 5: // Flood message
                        this.logger.log(new LogEntry("Received flood message from " + socket.getInetAddress().getHostAddress()));
                        t = new Thread(new NormalFloodWorker(this, p));
                        t.start();
                        break;
                    case 7: // LIVENESS CHECK message
                        //this.log(new LogEntry("Received liveness check from " + socket.getInetAddress().getHostAddress()));
                        t = new Thread(new LivenessCheckWorker(this, tcpConnection, p));
                        t.start();
                        break;
                    default:
                        this.logger.log(new LogEntry("Packet type not recognized. Message ignored!"));
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

    public String getStreamsDir(){
        return this.streamsDir;
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
    private Server s;

    public ServerWorker1(TCPConnection connection, Packet p, String RPIP, Server s){
        this.connection = connection;
        this.receivedPacket = p;
        this.RPIP = RPIP;
        this.s = s;
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

        String videoName = sr.getStreamName();

        // get video attributes....
        int videoLength = 100; // number of frames
        int frame_period = 75; // time between frames in ms.
        int video_extension = 26; //26 is Mjpeg type

        VideoMetadata vmd = new VideoMetadata(frame_period, videoName);
        // Convert VideoMetadata to bytes (serialize) and send the packet to RP (type 6 represents video metadata)
        Packet packetMetadata = new Packet(6, vmd.serialize());
        this.connection.send(packetMetadata);

        // Start the UDP video streaming. (Send directly to the RP)
        try {
            this.s.log(new LogEntry("Sent VideoMetadata packet to RP"));
            this.s.log(new LogEntry("Streaming '" + videoName + "' through UDP!"));
            Video video = new Video(this.s.getStreamsDir() + videoName);
            byte[] videoBuffer = null;
            while ((videoBuffer = video.getNextVideoFrame()) != null) {
                // Get the next frame of the video

                int frameNumber = video.getFrameNumber();
                //Builds a UDPDatagram object containing the frame
	            UDPDatagram udpDatagram = new UDPDatagram(video_extension, frameNumber, frameNumber*frame_period,
                 videoBuffer, videoBuffer.length, videoName);
                
                // get the bytes of the UDPDatagram
                byte[] packetBytes = udpDatagram.serialize();

	            // Initialize the DatagramPacket and send it over the UDP socket 
	            DatagramPacket senddp = new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName(this.RPIP), RP.RP_PORT);
	            this.ds.send(senddp);

                // Wait for 25 milliseconds before sending the next packet
                Thread.sleep(25);
            }
            this.s.log(new LogEntry("Sent " + video.getFrameNumber() + " frames!"));
            // notify the RP that the stream has ended
            Packet streamEndedNotification = new Packet(7);
            // Close the socket
            this.connection.send(streamEndedNotification);
            this.connection.stopConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
    
    public static String getExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return ""; // No extension found
        }
        return fileName.substring(lastIndexOfDot + 1);
    }
}
