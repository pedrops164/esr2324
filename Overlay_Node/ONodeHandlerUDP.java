package Overlay_Node;

import java.util.*;
import java.net.*;
import Common.*;

public class ONodeHandlerUDP implements Runnable {
    private DatagramSocket ds;
    private ONode oNode;
    private boolean running;
    
    public ONodeHandlerUDP(ONode oNode)
    {
        this.oNode = oNode;
        
        try {
            // open a socket for receiving UDP packets on the overlay node's port
            this.ds = new DatagramSocket(Util.PORT);
    
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

            this.oNode.log(new LogEntry("Listening on UDP:" + Util.PORT));
            this.running = true;
            while(this.running) {
                try {
                    // Receive the packet
                    this.ds.receive(receivedPacket);
                    //this.oNode.log(new LogEntry("Received UDP packet"));
                    
                    // Get the received bytes from the receivedPacket
                    byte[] receivedBytes = receivedPacket.getData();
                    
                    // Convert the received bytes into a Frame Packet
                    UDPDatagram datagram = UDPDatagram.deserialize(receivedBytes);
                    
                    if (oNode.alreadyStreaming(datagram.getStreamName())) {
                        // Get this of IP's of neighbours that want this stream
                        List<String> neighbourIps = this.oNode.getNeighbourIpsStream(datagram.getStreamName()); 
                        // For each neighbour send the UDPDatagram
                        for(String neighbourIp: neighbourIps){
                            DatagramPacket toSend = new DatagramPacket(receivedBytes, receivedBytes.length, 
                                InetAddress.getByName(neighbourIp), Util.PORT);
                            this.ds.send(toSend);
                            this.oNode.log(new LogEntry("Sent UDP packet"));
                        }
                    }
                } catch (java.io.StreamCorruptedException e) {
                    this.oNode.log(new LogEntry("Packet lost"));
                } catch (Exception e) {
                    e.printStackTrace();  
                }
                
            }
            
        } catch (Exception e) {
            if (this.running)
                e.printStackTrace();
        } finally {
            this.ds.close();
        }
    }

    public void turnOff()
    {
        this.running = false;;
        this.ds.close();
    }
}