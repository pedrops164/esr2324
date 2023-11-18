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
import Common.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client extends Node {

    private List<String> availableStreams;
    private RoutingTree routingTree;
    private Lock routingTreeLock;
    private Condition hasPaths;

    public Client(String args[], NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.availableStreams = new ArrayList<>();
        this.routingTree = new RoutingTree();
        this.routingTreeLock = new ReentrantLock();
        this.hasPaths = this.routingTreeLock.newCondition();
    }

    public void getAvailableStreams(){
        try{
            // Send the request
            Socket s = new Socket(this.RPIP, 333);
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
    
    public void showAvailableStreams(){
        int i = 1;
        System.out.println("Available streams to watch:");
        for(String stream : this.availableStreams){
            System.out.println("Stream " + Integer.toString(i) + ": " + stream);
            i++;
        }
        System.out.println("Select one to watch by inputing it's number:");
    }

    public void requestStreaming(int streamId){
        try{
            // Send request
            String stream = this.availableStreams.get(streamId-1);
            StreamRequest sr = new StreamRequest(stream, this.id);
            Socket s = new Socket(this.RPIP, 333);
            TCPConnection c = new TCPConnection(s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            sr.serialize(out);
            out.flush();
            byte [] data = baos.toByteArray();
            c.send(2, data); // Send the request to the RP
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void flood()
    {
        Path path = new Path(new PathNode(this.id, 333, this.ip));
        byte[] serializedPath = path.serialize();
        try
        {
            for (String neighbour : this.neighbours.values())
            {
                Socket s = new Socket(neighbour, 333);
                TCPConnection c = new TCPConnection(s);
                Packet p = new Packet(5, serializedPath);
                c.send(p);
                this.logger.log(new LogEntry("Sent flood message to " + neighbour + ":" + 333));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void run() throws InterruptedException, NoPathsAvailableException
    {
        // inicializar a receção por TCP
        Thread tcp = new Thread(new TCP_Worker(this));
        tcp.start();

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

        // obter o melhor caminho, tem que levar update sempre que receber caminhos novos
        Path bestPath = this.routingTree.getBestPath();

        // TODO : enviar o caminho junto com o pedido de stream
        this.getAvailableStreams();
        this.showAvailableStreams();
        Scanner in = new Scanner(System.in);
        int streamId = in.nextInt();
        this.requestStreaming(streamId);
        in.close();
    }
    public static void main(String args[]) throws InterruptedException, NoPathsAvailableException{
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        boolean debugMode = Arrays.stream(args).anyMatch(s -> s.equals("-g"));
        Client c = new Client(args, nr, debugMode);
        c.run();
    }
}   

class TCP_Worker implements Runnable
{
    private ServerSocket ss;
    private Client client;
    
    public TCP_Worker(Client client)
    {
        this.client = client;
        
        try 
        {
            this.ss = new ServerSocket(333);
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
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                
                switch(p.type)
                {
                    case 5: // Flood Message from client
                    this.client.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        Thread t = new Thread(new NormalFloodWorker(client, p));    
                        t.start();
                        break;
                    case 6: // Flood Response from RP
                        this.client.log(new LogEntry("Received flood response from RP: " + s.getInetAddress().getHostAddress()));
                        client.receivePath(p);
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