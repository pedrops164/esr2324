package Rendezvous_Point;

import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.StreamRequest;
import Common.LogEntry;
import Common.PathNode;
import Common.Util;

import java.net.*;

// Responsible to handle new video stream requests
class HandleStreamRequests implements Runnable{
    private RP rp;
    private TCPConnection neighbourConnection;
    private Packet receivedPacket;

    public HandleStreamRequests(TCPConnection neighbourConnection, Packet p, RP rp){
        this.rp = rp;
        this.neighbourConnection = neighbourConnection;
        this.receivedPacket = p;
    }

    public void requestStreamToServer(StreamRequest sr){
        try{
            String serverIP = this.rp.getServer(sr.getStreamName());
            Socket s = new Socket(serverIP, Util.PORT);
            TCPConnection serverConnection = new TCPConnection(s);
            byte [] data = sr.serialize();
            serverConnection.send(2, data); // Send the video stream request to the Server
            
            // Add the server to the streaming servers
            this.rp.addStreamingServer(sr.getStreamName(), serverIP);

            serverConnection.stopConnection();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        StreamRequest sr = StreamRequest.deserialize(data);
        boolean fixPath = sr.fixingPath();

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
        this.neighbourConnection.stopConnection();
    }    
}