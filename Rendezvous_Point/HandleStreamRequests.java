package Rendezvous_Point;

import Server.Server;

import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.StreamRequest;
import Common.LogEntry;

import java.io.*;
import java.net.*;

// Responsible to handle new video stream requests
// RPWorker2
class HandleStreamRequests implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;
    private int clientId;

    public HandleStreamRequests(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
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
            this.connection.send(metadataPacket);
            this.rp.log(new LogEntry("Received and sent VideoMetadata packet"));

            // Receive end of stream notification
            Packet p = serverConnection.receive();
            if (p.isEndOfStreamNotification()) {
                this.rp.log(new LogEntry("Received end of stream notification"));
            }
            serverConnection.stopConnection();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        StreamRequest sr = StreamRequest.deserialize(data);
        this.clientId = sr.getClientID();
        rp.addPathToClient(clientId, sr.getPath());

        // Adds the client to the data structure that maps streams to the clients watching them
        rp.addClientToStream(sr.getStreamName(), this.clientId);

        this.rp.log(new LogEntry("A client wants the stream: " + sr.getStreamName() + "!"));
        // Now we have to request to a server to stream this video
        this.requestStreamToServer(sr);

        this.connection.stopConnection();
    }    
}