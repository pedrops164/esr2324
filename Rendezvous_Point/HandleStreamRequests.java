package Rendezvous_Point;

import Server.Server;

import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.StreamRequest;
import Common.LogEntry;
import Common.PathNode;

import java.net.*;

// Responsible to handle new video stream requests
class HandleStreamRequests implements Runnable{
    private RP rp;
    private TCPConnection clientConnection;
    private Packet receivedPacket;

    public HandleStreamRequests(TCPConnection clientConnection, Packet p, RP rp){
        this.rp = rp;
        this.clientConnection = clientConnection;
        this.receivedPacket = p;
    }

    public void requestStreamToServer(StreamRequest sr){
        try{
            String serverIP = this.rp.getServer(sr.getStreamName());
            Socket s = new Socket(serverIP, Server.SERVER_PORT);
            TCPConnection serverConnection = new TCPConnection(s);
            byte [] data = sr.serialize();
            serverConnection.send(2, data); // Send the video stream request to the Server

            // Receive VideoMetadata through TCP and send to client
            Packet metadataPacket = serverConnection.receive();
            this.clientConnection.send(metadataPacket);
            this.rp.log(new LogEntry("Received and sent VideoMetadata packet"));

            // Receive end of stream notification
            Packet p = serverConnection.receive();
            if (p.type == 8) {
                this.rp.log(new LogEntry("Received end of stream notification"));
            }
            this.clientConnection.send(p);
            serverConnection.stopConnection();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        StreamRequest sr = StreamRequest.deserialize(data);

        PathNode previous = null;
        try{
            previous = sr.getPath().getPrevious(this.rp.getId());
        }catch (Exception e){
            e.printStackTrace();
        }
        this.rp.addStreamingFlux(sr.getStreamName(), previous.getNodeId());

        this.rp.log(new LogEntry("A client wants the stream: " + sr.getStreamName() + "!"));
        // Now we have to request to a server to stream this video
        this.requestStreamToServer(sr);
        this.clientConnection.stopConnection();
    }    
}