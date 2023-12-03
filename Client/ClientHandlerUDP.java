package Client;

import java.util.*;
import java.net.*;
import Common.*;
import Common.TCPConnection.Packet;

import Overlay_Node.ONode;

public class ClientHandlerUDP implements Runnable {
    private DatagramSocket ds;
    private Client client;
    
    public ClientHandlerUDP(Client client)
    {
        this.client = client;
        
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

            this.client.log(new LogEntry("Listening on UDP:" + this.client.getIp() + ":" + ONode.ONODE_PORT));

            while(true) {
                try {
                    // Receive the packet
                    this.ds.receive(receivedPacket);
                    //this.client.log(new LogEntry("Received UDP packet"));

                    // Get the received bytes from the receivedPacket
                    byte[] receivedBytes = receivedPacket.getData();

                    UDPDatagram udpDatagram = UDPDatagram.deserialize(receivedBytes);
                    this.client.cvm.addFrame(udpDatagram);
                } catch (java.awt.HeadlessException e) {
                    this.client.log(new LogEntry("Must run 'export DISPLAY=:0.0' before running the client"));
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();  
        } finally {
            this.ds.close();
        }
    }
}