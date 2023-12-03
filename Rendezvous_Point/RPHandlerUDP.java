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
    private int numThreads;
    private Thread[] threadPool;
    private RPDatagramPacketQueue datagramPacketQueue;

    public RPHandlerUDP(RP rp){
        this.rp = rp;
        try {
            // open a socket for receiving UDP packets on RP's port
            this.ds = new DatagramSocket(RP.RP_PORT);
        } catch(Exception e){
            e.printStackTrace();
        }

        this.datagramPacketQueue = new RPDatagramPacketQueue(5);
        this.numThreads = 4;
        this.threadPool = new Thread[this.numThreads];

        for (int i=0 ; i<this.numThreads ; i++)
            this.threadPool[i] = new Thread(new RPThreadPoolWorker(i+1, rp, this.datagramPacketQueue, this.ds));
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

                DatagramPacket copy = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());
                this.datagramPacketQueue.pushPacket(copy);
                this.rp.log(new LogEntry("Sent DatagramPacket to Thread pool queue"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.ds.close();
        }
    }    
}