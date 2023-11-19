package Server;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.Node;
import Common.NormalFloodWorker;
import Common.ServerStreams;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Rendezvous_Point.RP;
//import Common.VideoStream;
//import Common.RTPpacket;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Node {
    private ServerSocket ss;

    private List<String> streams;

    public Server(String []args, NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.streams = new ArrayList<>();

        try{
            this.ss = new ServerSocket(333);
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

        for(int i=2; i<args.length; i++){
            File path = new File(args[i]);
            File [] listOfFiles = path.listFiles();
            for(File f : listOfFiles){
                if(f.isFile()){
                    this.streams.add(f.getName());
                }
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

                // Answer to the request
                Packet p = c.receive();
                ByteArrayInputStream bais = new ByteArrayInputStream(p.data);
                DataInputStream in = new DataInputStream(bais);
                String resp = in.readUTF();
                this.logger.log(new LogEntry("Received Response to Available Streams notification to Rendezvous Point: " +  resp));
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
                System.out.println("Server waiting for new requests!");
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    case 2: // New video stream request
                        this.logger.log(new LogEntry("Received Video Stream Request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new ServerWorker1(p, this.RPIP, this));
                        t.start();
                        break;
                    case 5: // Flood message
                        this.logger.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new NormalFloodWorker(this, p));
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
    private Packet receivedPacket;
    private String RPIP;
    private DatagramSocket RTPsocket;
    private Node node;

    public ServerWorker1(Packet p, String RPIP, Node node){
        this.receivedPacket = p;
        this.RPIP = RPIP;
        this.node = node;
        try {
            this.RTPsocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        StreamRequest sr = StreamRequest.deserialize(in);

        String videoPath = sr.getStreamName();

        // get video attributes....
        int videoLength = 100; // number of frames
        int frame_period = 100; // time between frames in ms.
        int video_extension = 26; //26 is Mjpeg type
        

        // Start the UDP video streaming. (Send directly to the RP)
        try {
            this.node.log(new LogEntry("Streaming '" + videoPath + "' through UDP!"));
            // VideoStream video = new VideoStream(videoPath);
            // byte[] videoBuffer = new byte[15000]; //allocate memory for the sending buffer
            // for (int frameNumber = 0; frameNumber < videoLength; frameNumber++) {
	        //     int image_length = video.getnextframe(videoBuffer);
            //     //Builds an RTPpacket object containing the frame
	        //     RTPpacket rtp_packet = new RTPpacket(video_extension, frameNumber, frameNumber*frame_period, videoBuffer, image_length);

	        //     //get to total length of the full rtp packet to send
	        //     int packet_length = rtp_packet.getlength();

	        //     //retrieve the packet bitstream and store it in an array of bytes
	        //     byte[] packet_bits = new byte[packet_length];
	        //     rtp_packet.getpacket(packet_bits);

	        //     //send the packet as a DatagramPacket over the UDP socket 
	        //     DatagramPacket senddp = new DatagramPacket(packet_bits, packet_length, InetAddress.getByName(this.RPIP), RP.RP_PORT);
	        //     this.RTPsocket.send(senddp);

	        //     //rtp_packet.printheader();
            //     //System.out.println("Sent video frame " + frameNumber);

            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
        

    }    
}
