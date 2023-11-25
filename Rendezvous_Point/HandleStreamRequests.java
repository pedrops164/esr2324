package Rendezvous_Point;

import Server.Server;
import Overlay_Node.ONode;
import Rendezvous_Point.RP;

import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Path;
import Common.UDPDatagram;
import Common.FramePacket;
import Common.StreamRequest;
import Common.LogEntry;


import java.io.*;
import java.net.*;

// Responsible to handle new video stream requests
// RPWorker2
class HandleStreamRequests implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private DatagramSocket ds;
    private Packet receivedPacket;
    private int clientId;

    public HandleStreamRequests(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
        try {
            // open a socket for receiving UDP packets on RP's port
            this.ds = new DatagramSocket(RP.RP_PORT);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void requestStreamToServer(StreamRequest sr){
        try{
            String serverIP = this.rp.getServer(sr.getStreamName());
            Socket s = new Socket(serverIP, Server.SERVER_PORT);
            TCPConnection c = new TCPConnection(s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            sr.serialize(out);
            out.flush();
            byte [] data = baos.toByteArray();
            c.send(2, data); // Send the video stream request to the Server

            // Receive VideoMetadata through TCP and send to client
            Packet metadataPacket = c.receive();
            this.connection.send(metadataPacket);
            this.rp.log(new LogEntry("Received and sent VideoMetadata packet"));

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void receiveUDPpackets() {
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
                // Get the pest path to the client
                Path bestPath = rp.paths.get(this.clientId);
                // get the id of the next node in the path to the client
                int nextNodeId = bestPath.nextNode(this.rp.getId());
                this.rp.log(new LogEntry("Received UDP packet"));

                // Now Send UDP packet to the next Overlay Node in the path of the client

                // get the bytes from the UDP packet, and convert them into UDPDatagram
                byte[] receivedBytes = receivePacket.getData();
                // build the UDPDatagram from the received bytes (deserialize the bytes)
                UDPDatagram receivedPacket = UDPDatagram.deserialize(receivedBytes);
                // build the FramePacket to send to the client
                FramePacket fp = new FramePacket(bestPath, receivedPacket);

                // Serialize FramePacket
                byte[] fpBytes = fp.serialize();

                // Get neighbor ip and port (his id is nextNodeId), and send the udp packet to him
                String neighbourIp = this.rp.getNeighbourIp(nextNodeId);
                // Get the UDP packet to send with the bytes from the frame packet
                DatagramPacket udpFramePacket = new DatagramPacket(fpBytes, fpBytes.length, InetAddress.getByName(neighbourIp), ONode.ONODE_PORT);
                // Send the DatagramPacket through the UDP socket
                this.ds.send(udpFramePacket);
                this.rp.log(new LogEntry("Sent UDP packet"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.ds.close();
        }
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        StreamRequest sr = StreamRequest.deserialize(in);
        this.clientId = sr.getClientID();

        try 
        {
            this.rp.log(new LogEntry("A client wants the stream: " + sr.getStreamName() + "!"));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        // Now we have to request to a server to stream this video
        this.requestStreamToServer(sr);

        this.receiveUDPpackets();

        // Now we receive the UDP video stream

        this.connection.stopConnection();

    }    
}