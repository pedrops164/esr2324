package Rendezvous_Point;

import Common.LogEntry;

import java.net.*;
import java.util.*;

// Responsible to handle new video stream requests
// RPWorker2
class RPHandlerUDP implements Runnable{
    private RP rp;
    private DatagramSocket ds;
    private int numThreads, pushQuantity;
    private Thread[] threadPool;
    private RPDatagramPacketQueue datagramPacketQueue;

    public RPHandlerUDP(RP rp){
        this.rp = rp;
        try {
            // open a socket for receiving UDP packets on RP's port
            this.ds = new DatagramSocket(RP.RP_PORT);
            this.ds.setSoTimeout(5000);
        } catch(Exception e){
            e.printStackTrace();
        }

        this.datagramPacketQueue = new RPDatagramPacketQueue();
        this.numThreads = 4;
        this.pushQuantity = 5; // mudar isto 
        this.threadPool = new Thread[this.numThreads];

        for (int i=0 ; i<this.numThreads ; i++)
            this.threadPool[i] = new Thread(new RPThreadPoolWorker(i+1, rp, this.datagramPacketQueue, this.ds));
    }
    
    public void run(){
        List<DatagramPacket> packetsToPush = new ArrayList<>();
        try {
            // set the buffer size
            int buffersize = 15000;
            // create the buffer to receive the packets
            byte[] receiveData = new byte[buffersize];
    
            this.rp.log(new LogEntry("Listening on UDP in Port " + RP.RP_PORT));
            // Create the packet which will receive the data
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);


            for (Thread thread : this.threadPool)
                thread.start();
    
            while(true) {
                try {
                    // Receive the packet
                    this.ds.receive(receivePacket);

                    DatagramPacket copy = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());
                    packetsToPush.add(copy);
                    //this.rp.log(new LogEntry("Received UDP packet"));

                    if (packetsToPush.size() >= this.pushQuantity)
                    {
                        this.datagramPacketQueue.pushPackets(packetsToPush);
                        packetsToPush.clear();
                        this.rp.log(new LogEntry("Sent " + this.pushQuantity + " UDP packets to Thread pool queue"));
                    }
                } catch (SocketTimeoutException e) {
                    if (packetsToPush.size() > 0){
                        this.datagramPacketQueue.pushPackets(packetsToPush);
                        packetsToPush.clear();
                        this.rp.log(new LogEntry("Sent " + this.pushQuantity + " UDP packets to Thread pool queue"));
                        this.rp.log(new LogEntry("Timeout reached. No UDP packets received for 5 seconds. Sent remaining packets"));
                    }
                    
                }
            }
        }  catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.ds.close();
        }
    }    
}