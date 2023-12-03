package Rendezvous_Point;

import Common.Path;
import Common.UDPDatagram;
import Common.FramePacket;
import Common.LogEntry;

import Overlay_Node.ONode;

import java.net.*;
import java.util.*;

// Responsible to handle new video stream requests
// RPWorker2
class RPHandlerUDP implements Runnable{
    private RP rp;
    private DatagramSocket ds;

    public RPHandlerUDP(RP rp){
        this.rp = rp;
        try {
            // open a socket for receiving UDP packets on RP's port
            this.ds = new DatagramSocket(RP.RP_PORT);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public void run(){
        try {
            // set the buffer size
            int buffersize = 15000;
            // create the buffer to receive the packets
            byte[] receiveData = new byte[buffersize];
    
            this.rp.log(new LogEntry("Listening on UDP in Port " + RP.RP_PORT));
            // Create the packet which will receive the data
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    
            while(true) {
                // Receive the packet
                this.ds.receive(receivePacket);
                this.rp.log(new LogEntry("Received UDP packet"));

                // get the bytes from the UDP packet, and convert them into UDPDatagram
                byte[] receivedBytes = receivePacket.getData();
                // build the UDPDatagram from the received bytes (deserialize the bytes)
                UDPDatagram receivedPacket = UDPDatagram.deserialize(receivedBytes);
                System.out.println("Recebi pacote UDP da stream: " + receivedPacket.getStreamName());
                
                List<String> neighbourIps = this.rp.getNeighbourIpsStream(receivedPacket.getStreamName());
                for (String neighbourIp: neighbourIps) {
                    DatagramPacket toSend = new DatagramPacket(receivedBytes, receivedBytes.length, 
                                    InetAddress.getByName(neighbourIp), ONode.ONODE_PORT);
                    // Send the DatagramPacket through the UDP socket
                    this.ds.send(toSend);
                    this.rp.log(new LogEntry("Sent UDP packet"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.ds.close();
        }
    }    
}