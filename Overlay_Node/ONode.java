package Overlay_Node;

import java.util.Arrays;
import java.util.Map;
import java.net.*;
import java.io.*;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.FramePacket;
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
        Thread udp = new Thread(new UDP_Worker(this));
        udp.start();
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
    private DatagramSocket ds;
    private ONode node;
    
    public UDP_Worker(ONode node)
    {
        this.node = node;
        
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

            this.node.log(new LogEntry("Listening on UDP:" + this.node.getIp() + ":" + ONode.ONODE_PORT));
            while(true) {
                // Receive the packet
                this.ds.receive(receivedPacket);
                this.node.log(new LogEntry("Received UDP packet"));

                // Get the received bytes from the receivedPacket
                byte[] receivedBytes = receivedPacket.getData();

                // Convert the received bytes into a Frame Packet
                ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
                DataInputStream in = new DataInputStream(bais);
                FramePacket fp = FramePacket.deserialize(in);

                // get the id of the next node in the Path to the client
                int nextNodeId = fp.getPath().nextNode(this.node.getId());
                //get next node's ip
                String neighbourIp = this.node.getNeighbourIp(nextNodeId);

                // get the bytes of the FramePacket
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                fp.serialize(out);
                out.flush();
                byte[] fpBytes = baos.toByteArray();

                // Change the destination address of the packet to send
                DatagramPacket packetToSend = new DatagramPacket(fpBytes, fpBytes.length, InetAddress.getByName(neighbourIp), ONode.ONODE_PORT);
                // Send the DatagramPacket through the UDP socket
                this.ds.send(packetToSend);
                this.node.log(new LogEntry("Sent UDP packet"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();  
        } finally {
            this.ds.close();
        }
    }
}