package Overlay_Node;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.LivenessCheckWorker;
import Common.Node;
import Common.PathNode;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.UDPDatagram;
import Common.VideoMetadata;
import Common.TCPConnection.Packet;
import Common.NormalFloodWorker;

import java.net.*;
import java.util.*;

public class ONode extends Node {
    public static int ONODE_PORT = 333;
    private Map<String, List<Integer>> streamNeighours;

    public ONode(int id, NeighbourReader nr, boolean debugMode)
    {
        super(id, nr, debugMode);
        this.streamNeighours = new HashMap<>();
    }

    public int getId()
    {
        return id;
    } 

    public Map<Integer, String> getNeighbours()
    {
        return neighbours;
    }

    public boolean alreadyStreaming(String stream){
        if(this.streamNeighours.containsKey(stream)) 
            return true;
        
        return false;
    }

    public void addStreamingFlux(String streamName, int neighbour){

        if(this.streamNeighours.containsKey(streamName)){
            this.streamNeighours.get(streamName).add(neighbour);
        }else{
            List<Integer> neighbours = new ArrayList<>();
            neighbours.add(neighbour);
            this.streamNeighours.put(streamName, neighbours);
        }
    }

    public List<String> getNeighbourIpsStream(String streamName){
        List<Integer> neighbours = this.streamNeighours.get(streamName);
        return this.getNeighboursIps(neighbours);
    }

    public void run()
    {
        try {
            // Launch tcp worker
            Thread tcp = new Thread(new TCP_Worker(this));
            tcp.start();
            // Launch udp worker
            Thread udp = new Thread(new UDP_Worker(this));
            udp.start();

            // join threads
            tcp.join();
            udp.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]){
        int id = Integer.parseInt(args[0]);
        NeighbourReader nr = new NeighbourReader(id, args[1]);
        boolean debugMode = Arrays.stream(args).anyMatch(s -> s.equals("-g"));
        ONode onode = new ONode(id, nr, debugMode);
        onode.run();
    }
}

class TCP_Worker implements Runnable
{
    private ServerSocket ss;
    private ONode oNode;
    
    public TCP_Worker(ONode node)
    {
        this.oNode = node;
        
        try 
        {
            this.ss = new ServerSocket(ONode.ONODE_PORT);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();    
        }
    }
    
    @Override
    public void run() 
    {
        try
        {
            this.oNode.log(new LogEntry("Now Listening to TCP requests"));
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;
                String address = s.getInetAddress().getHostAddress();
                
                switch(p.type)
                {   
                    case 2: // New stream request from a client
                        this.oNode.log(new LogEntry("New streaming request!"));
                        t = new Thread(new HandleStreamingRequest(this.oNode, p, c));
                        t.start();
                        break;
                    case 5: // Flood Message 
                        this.oNode.log(new LogEntry("Received flood message from " + address));
                        t = new Thread(new NormalFloodWorker(this.oNode, p));    
                        t.start();
                        break;
                    case 7: // ALIVE? message
                        //this.oNode.log(new LogEntry("Received liveness check from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new LivenessCheckWorker(this.oNode, c, p));
                        t.start();
                        break;
                    default:
                        this.oNode.log(new LogEntry("Packet type < " + p.type + " > not recognized. Message ignored!"));
                        c.stopConnection();
                        break;
                }
            }
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

class HandleStreamingRequest implements Runnable{
    private ONode oNode;
    private TCPConnection c;
    private StreamRequest sr;

    public HandleStreamingRequest(ONode oNode, Packet p, TCPConnection c){
        this.oNode = oNode;
        this.c = c;
        this.sr = StreamRequest.deserialize(p.data);
    }

    public void addFlux(){
        PathNode previous = null;
        try{
            previous = this.sr.getPath().getPrevious(this.oNode.getId());
        }catch (Exception e){
            e.printStackTrace();
        }
        this.oNode.addStreamingFlux(this.sr.getStreamName(), previous.getNodeId());
    }

    public void run(){

        // This overlay node is not streaming is stream at the moment
        if(!this.oNode.alreadyStreaming(this.sr.getStreamName())){
            addFlux();
            this.oNode.log(new LogEntry("This Overlay Node isn't streaming: " + this.sr.getStreamName()));
            // Send the streaming request to the next node in the path
            try{
                this.oNode.log(new LogEntry("Resending the streaming request along the path!"));
                PathNode nextNode = this.sr.getPath().getNext(this.oNode.getId());
                Socket s = new Socket(nextNode.getNodeIPAddress().toString(), ONode.ONODE_PORT);
                TCPConnection nextC = new TCPConnection(s);
                byte[] srBytes = sr.serialize();
                nextC.send(2, srBytes); // Send the request to the next node in the path

                // Receive and send the video metadata info
                Packet p = nextC.receive();
                this.c.send(p);
                this.oNode.log(new LogEntry("Received and sent video metadata!"));

                // Receive and send end of stream notification
                Packet endOfStreamNotification = nextC.receive();
                this.c.send(endOfStreamNotification);
                this.oNode.log(new LogEntry("Received end of stream notification!"));

                this.c.stopConnection();
            }catch(Exception e){
                e.printStackTrace();
            }   
        }else{
            addFlux();
            this.oNode.log(new LogEntry("This Overlay Node is already streaming: " + this.sr.getStreamName()));;
            // Tenho que enviar o video metadata daqui? Onde vou buscar o frame_period?
            try{
                this.oNode.log(new LogEntry("Sending video metadata!"));
                VideoMetadata vm = new VideoMetadata(100, this.sr.getStreamName());
                byte[] vmBytes = vm.serialize();
                this.c.send(6, vmBytes); // Send the request to the next node in the path
                this.c.stopConnection();
            }catch(Exception e){
                e.printStackTrace();
            } 
        }
    }
}

class UDP_Worker implements Runnable {
    private DatagramSocket ds;
    private ONode oNode;
    
    public UDP_Worker(ONode oNode)
    {
        this.oNode = oNode;
        
        try {
            // open a socket for receiving UDP packets on the overlay node's port
            this.ds = new DatagramSocket(ONode.ONODE_PORT);
    
        } catch (Exception e) {
            e.printStackTrace();    
        }
    }

    @Override
    public void run() 
    {
        try {
            // set the buffer size
            int buffersize = 15000;
            // create the buffer to receive the packets
            byte[] receiveData = new byte[buffersize];
            // Create the packet which will receive the data
            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);

            this.oNode.log(new LogEntry("Listening on UDP:" + this.oNode.getIp() + ":" + ONode.ONODE_PORT));
            while(true) {
                // Receive the packet
                this.ds.receive(receivedPacket);
                this.oNode.log(new LogEntry("Received UDP packet"));

                // Get the received bytes from the receivedPacket
                byte[] receivedBytes = receivedPacket.getData();

                // Convert the received bytes into a Frame Packet
                UDPDatagram datagram = UDPDatagram.deserialize(receivedBytes);
                
                // Get this of IP's of neighbours that want this stream
                List<String> neighbourIps = this.oNode.getNeighbourIpsStream(datagram.getStreamName()); 
                // For each neighbour send the UDPDatagram
                for(String neighbourIp: neighbourIps){
                    DatagramPacket toSend = new DatagramPacket(receivedBytes, receivedBytes.length, 
                        InetAddress.getByName(neighbourIp), ONode.ONODE_PORT);
                    this.ds.send(toSend);
                    this.oNode.log(new LogEntry("Sent UDP packet"));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();  
        } finally {
            this.ds.close();
        }
    }
}