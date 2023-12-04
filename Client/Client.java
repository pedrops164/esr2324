package Client;

import Common.NeighbourReader;
import Common.Node;
import Common.NormalFloodWorker;
import Common.Path;
import Common.PathNode;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.LogEntry;
import Common.UDPDatagram;
import Common.VideoMetadata;
import Common.Util;

import Overlay_Node.ONode;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client extends Node {
    private List<String> availableStreams;
    private RoutingTree routingTree;
    private Lock routingTreeLock;
    private Condition hasPaths;
    ClientVideoManager cvm;

    public Client(String args[], boolean debugMode, String bootstrapperIP){
        super(-1, debugMode, bootstrapperIP);
        this.availableStreams = new ArrayList<>();
        this.routingTree = new RoutingTree();
        this.routingTreeLock = new ReentrantLock();
        this.hasPaths = this.routingTreeLock.newCondition();
        this.cvm = new ClientVideoManager(this);
    }

    public void getAvailableStreams(){
        try{
            // Send the request
            Socket s = new Socket(this.RPIPs.get(0), Util.PORT);
            TCPConnection c = new TCPConnection(s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Available streams.");
            out.flush();
            byte [] data = baos.toByteArray();
            c.send(3, data); // Send the request to the RP


            // Answer to the request
            Packet p = c.receive();
            ByteArrayInputStream bais = new ByteArrayInputStream(p.data);
            DataInputStream in = new DataInputStream(bais);
            int nr = in.readInt();
            for(int i=0; i<nr; i++){
                this.availableStreams.add(in.readUTF());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public List<Path> getOrderedPaths()
    {
        this.routingTreeLock.lock();
        try
        {
            return this.routingTree.getOrderedPaths();
        }
        finally
        {
            this.routingTreeLock.unlock();
        }
    }

    public void receivePath(Packet packet)
    {
        this.routingTreeLock.lock();
        try
        {
            Path path = Path.deserialize(packet.data);
            this.routingTree.addPath(path);
            this.hasPaths.signalAll();
        }
        finally
        {
            this.routingTreeLock.unlock();
        }
    }

    public void removePath(Path p)
    {
        this.routingTreeLock.lock();
        try
        {
            this.routingTree.removePath(p);
        }
        finally
        {
            this.routingTreeLock.unlock();
        }
    }
    
    public void showAvailableStreams(){
        int i = 1;
        System.out.println("Available streams to watch:");
        for(String stream : this.availableStreams){
            System.out.println("Stream " + Integer.toString(i) + ": " + stream);
            i++;
        }
        System.out.println("Select one to watch by inputing it's number:");
    }

    // Method responsible to request the UDP streaming 
    public void requestStreaming(int streamId){
        try{
            // get the best path
            Path path;
            this.routingTreeLock.lock();
            try
            {
                path = this.routingTree.getBestPath();
            }
            finally
            {
                this.routingTreeLock.unlock();
            }
            // byte[] serializedPath = path.serialize();

            // Send request
            String stream = this.availableStreams.get(streamId-1);
            this.logger.log(new LogEntry("Client requesting stream: " + stream));
            StreamRequest sr = new StreamRequest(stream, this.id, path);

            // Send the request through TCP to the next node in the path
            PathNode nextNode = path.getNext(this.id);
            Socket s = new Socket(nextNode.getNodeIPAddress().toString(), Util.PORT);
            TCPConnection neighbourConnection = new TCPConnection(s);
            byte[] srBytes = sr.serialize();
            neighbourConnection.send(2, srBytes); // Send the request to the next node in the path

            // Receive VideoMetadata through TCP and send to client
            //Packet metadataPacket = neighbourConnection.receive();
            //this.logger.log(new LogEntry("Received Video Metadata!"));
            //byte[] metadata = metadataPacket.data;
            //VideoMetadata vmd = VideoMetadata.deserialize(metadata);
            
            // Set the frame period of the Video Player respective to the stream of this metadata
            //this.cvm.updateVideoInfo(vmd);

            // Receive end of stream packet
            //Packet endOfStreamPacket = neighbourConnection.receive();
            //this.logger.log(new LogEntry("Received End of Stream Notification!"));

            // Notify the video manager that this stream has ended (no more packets will be received)
            //this.cvm.streamEnded(stream);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void flood()
    {
        Path path = new Path(new PathNode(this.id, Util.PORT, this.ip));
        byte[] serializedPath = path.serialize();
        for (String neighbour : this.neighbours.values())
        {
            if (neighbour.equals(this.ip))
                continue;
            try
            {
                Socket s = new Socket(neighbour, Util.PORT);
                TCPConnection c = new TCPConnection(s);
                Packet p = new Packet(5, serializedPath);
                c.send(p);
                c.stopConnection();
                this.logger.log(new LogEntry("Sent flood message to " + neighbour + ":" + Util.PORT));
            }
            catch (Exception eFromSocket)
            {
                this.logger.log(new LogEntry("Error sending flood message to " + neighbour + ". Retrying later"));
            }
        }
    }

    public void run() throws InterruptedException, NoPathsAvailableException
    {
        boolean successfull = this.messageBootstrapper();

        // inicializar a receção por TCP
        Thread tcp = new Thread(new ClientHandlerTCP(this));
        tcp.start();
        Thread udp = new Thread(new ClientHandlerUDP(this));
        udp.start();
        
        // executar o flood
        this.flood();
        
        // enquanto não houver caminhos esperar
        this.routingTreeLock.lock();
        try
        {
            while (!this.routingTree.isAvailable())
            this.hasPaths.await();
        }
        finally
        {
            this.routingTreeLock.unlock();
        }
        
        Thread pathManager = new Thread(new ClientPathManager(this));
        pathManager.start();

        this.getAvailableStreams();
        this.showAvailableStreams();
        Scanner in = new Scanner(System.in);
        int streamId = in.nextInt();
        this.requestStreaming(streamId);
        in.close();
    }

    public static void main(String args[]) throws InterruptedException, NoPathsAvailableException{
        List<String> argsL = new ArrayList<>();
        boolean debugMode = false;

        for (int i=0 ; i<args.length ; i++)
        {
            String arg = args[i];
            if (arg.equals("-g"))
                debugMode = true;
            else
                argsL.add(arg);
        }

        args = argsL.toArray(new String[0]);
        Client c = new Client(args, debugMode, args[0]);
        c.run();
    }
}   
