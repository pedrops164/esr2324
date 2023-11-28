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

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        StreamRequest sr = StreamRequest.deserialize(in);
        this.clientId = sr.getClientID();

        // Adds the client to the data structure that maps streams to the clients watching them
        rp.addClientToStream(sr.getStreamName(), this.clientId);

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

        this.connection.stopConnection();

    }    
}