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

                // Now Send UDP packet to the next Overlay Node in the path of the client

                // get the bytes from the UDP packet, and convert them into UDPDatagram
                byte[] receivedBytes = receivePacket.getData();
                // build the UDPDatagram from the received bytes (deserialize the bytes)
                UDPDatagram receivedPacket = UDPDatagram.deserialize(receivedBytes);

                // Create list of best paths to the clients
                //Path bestPath = rp.paths.get(this.clientId);
                List<Path> paths = new ArrayList<Path>();
                // We iterate through every client who wants this stream, add we add the paths to them to the paths list
                for (int clientId: rp.getStreamClients(receivedPacket.getStreamName())) {
                    paths.add(rp.paths.get(clientId));
                }
                
                // build the FramePacket to send to the client
                FramePacket fp = new FramePacket(paths, receivedPacket);

                // Serialize FramePacket
                byte[] fpBytes = fp.serialize();
                
                // Create a Set that will contain the ids of the neighbors that will receive the packet
                Set<Integer> neighborIds = new HashSet<>();
                /*
                 * For every client that wants this stream, we go through their paths and get the id of 
                 * the next node.
                 * After having the id of every 'next node', we get the ip of these nodes. We store the ids of
                 * the nodes in a Set because it doesn't allow repeated elements, and we don't want to send
                 * the same packet twice to the same neighbor (multicast).
                 */
                for (Path path: paths) {
                    int nextNodeId = path.nextNode(this.rp.getId());
                    neighborIds.add(nextNodeId);
                }
                for (int nextNodeId: neighborIds) {
                    // Get neighbor ip (his id is nextNodeId)
                    String neighbourIp = this.rp.getNeighbourIp(nextNodeId);
                    DatagramPacket udpFramePacket = new DatagramPacket(fpBytes, fpBytes.length, 
                                    InetAddress.getByName(neighbourIp), ONode.ONODE_PORT);
                    // Send the DatagramPacket through the UDP socket
                    this.ds.send(udpFramePacket);
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