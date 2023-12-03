package Rendezvous_Point;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import Common.LogEntry;
import Common.UDPDatagram;
import Overlay_Node.ONode;

public class RPThreadPoolWorker implements Runnable{

    private int workerID;
    private RP rp;
    private RPDatagramPacketQueue datagramPacketQueue;
    private DatagramSocket datagramSocket;

    public RPThreadPoolWorker(int workerID, RP rp, RPDatagramPacketQueue datagramPacketQueue, DatagramSocket datagramSocket)
    {
        this.workerID = workerID;
        this.rp = rp;
        this.datagramPacketQueue = datagramPacketQueue;
        this.datagramSocket = datagramSocket;
    }


    @Override
    public void run() {
        
        while (true)
        {
            DatagramPacket packet = this.datagramPacketQueue.popPackets();
            this.rp.log(new LogEntry("Thread pool worker " + this.workerID + " is handling an UDP packet."));
            
            try
            {
                // get the bytes from the UDP packet, and convert them into UDPDatagram
                byte[] receivedBytes = packet.getData();
                // build the UDPDatagram from the received bytes (deserialize the bytes)
                UDPDatagram receivedPacket = UDPDatagram.deserialize(receivedBytes);
                
                List<String> neighbourIps = this.rp.getNeighbourIpsStream(receivedPacket.getStreamName());
                for (String neighbourIp: neighbourIps) {
                    DatagramPacket toSend = new DatagramPacket(receivedBytes, receivedBytes.length, 
                                    InetAddress.getByName(neighbourIp), ONode.ONODE_PORT);
                    // Send the DatagramPacket through the UDP socket
                    this.datagramSocket.send(toSend);
                    this.rp.log(new LogEntry("Thread pool worker " + this.workerID + " sent UDP packet to " + neighbourIp));
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
}