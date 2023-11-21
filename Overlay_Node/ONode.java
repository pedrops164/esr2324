package Overlay_Node;

import java.util.Arrays;
import java.util.Map;
import java.net.*;
import java.io.*;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.Node;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.NormalFloodWorker;

public class ONode extends Node {
    public static int ONODE_PORT = 333;

    public ONode(int id, NeighbourReader nr, boolean debugMode)
    {
        super(id, nr, debugMode);
    }

    public int getId()
    {
        return id;
    } 

    public Map<Integer, String> getNeighbours()
    {
        return neighbours;
    }

    public void run()
    {
        Thread tcp = new Thread(new TCP_Worker(this));
        tcp.start();
        //Thread udp = new Thread(new UDP_Worker(this));
        //udp.start();
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
    private Node node;
    
    public TCP_Worker(Node node)
    {
        this.node = node;
        
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
            this.node.log(new LogEntry("Now Listening to TCP requests"));
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                
                switch(p.type)
                {
                    case 5: // Flood Message 
                        this.node.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        Thread t = new Thread(new NormalFloodWorker(node, p));    
                        t.start();
                        break;
                    default:
                        this.node.log(new LogEntry("Packet type < " + p.type + " > not recognized. Message ignored!"));
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

class UDP_Worker implements Runnable {
    private DatagramSocket ss;
    private Node node;
    
    public UDP_Worker(Node node)
    {
        this.node = node;
        
        try {
            // open a socket for receiving UDP packets on the overlay node's port
            this.ss = new DatagramSocket(ONode.ONODE_PORT);
    
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
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            this.node.log(new LogEntry("Listening on UDP:" + InetAddress.getLocalHost().getHostAddress() + ":" + ONode.ONODE_PORT));
            while(true) {
                // Receive the packet
                this.ss.receive(receivePacket);
                this.node.log(new LogEntry("Received UDP packet"));
            }
            
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            this.ss.close();
        }
    }
}