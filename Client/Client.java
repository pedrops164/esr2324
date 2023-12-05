package Client;

import Common.Node;
import Common.Path;
import Common.PathNode;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.LogEntry;
import Common.Util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client extends Node {
    private ClientGUI clientGUI;
    private List<String> availableStreams;
    private RoutingTree routingTree;
    private Lock routingTreeLock;
    private Condition hasPaths;
    ClientVideoManager cvm;
    private ClientHandlerTCP clientHandlerTCP;
    private ClientHandlerUDP clientHandlerUDP;
    private ClientPathManager clientPathManager;

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
            // Reset the array of available streams
            this.availableStreams = new ArrayList<>();

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
        int exit = this.availableStreams.size() + 1;
        System.out.println("Press " + exit + " to exit.");
        System.out.println("Select one to watch by inputing it's number or exit:");
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
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void flood()
    {
        Path path = new Path(new PathNode(this.id, Util.PORT, this.ips.get(0)));
        byte[] serializedPath = path.serialize();
        for (String neighbour : this.neighbours.values())
        {
            if (this.ips.contains(neighbour))
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
        this.log(new LogEntry("Sending neighbour request to Bootstrapper"));
        int successfull = this.messageBootstrapper();

        if (successfull == 1)
        {
            System.out.println("Bootstrapper is not available.. Shutting down");
            return;
        }
        else if (successfull == 2)
        {
            System.out.println("This node is not on the overlay network.. Shutting down");
            return;
        }

        // inicializar a receção por TCP
        this.clientHandlerTCP = new ClientHandlerTCP(this);
        Thread tcp = new Thread(this.clientHandlerTCP);
        tcp.start();
        this.clientHandlerUDP = new ClientHandlerUDP(this);
        Thread udp = new Thread(this.clientHandlerUDP);
        udp.start();
        
        // executar o flood
        this.flood();

        this.clientPathManager = new ClientPathManager(this);
        Thread pathManager = new Thread(this.clientPathManager);
        pathManager.start();
        
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
        

        this.getAvailableStreams();
        this.clientGUI = new ClientGUI(this);
    }

    public List<String> getAvailableStreamsList(){
        return this.availableStreams;
    }

    public void turnOff()
    {
        this.clientHandlerTCP.turnOff();
        this.clientHandlerUDP.turnOff();
        this.clientPathManager.turnOff();
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
