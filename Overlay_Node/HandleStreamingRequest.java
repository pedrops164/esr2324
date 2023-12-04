package Overlay_Node;

import Common.UDPDatagram;
import Common.LogEntry;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.PathNode;
import Common.StreamRequest;
import Common.VideoMetadata;
import Common.Util;

import java.net.*;
import java.util.*;

class HandleStreamingRequest implements Runnable{
    private ONode oNode;
    private TCPConnection c;
    private StreamRequest sr;

    public HandleStreamingRequest(ONode oNode, Packet p, TCPConnection c){
        this.oNode = oNode;
        this.c = c;
        this.sr = StreamRequest.deserialize(p.data);
    }

    public void addFlux(){
        PathNode previous = null;
        try{
            previous = this.sr.getPath().getPrevious(this.oNode.getId());
        }catch (Exception e){
            e.printStackTrace();
        }
        this.oNode.addStreamingFlux(this.sr.getStreamName(), previous.getNodeId());
    }

    public void run(){

        // This overlay node is not streaming is stream at the moment
        if(!this.oNode.alreadyStreaming(this.sr.getStreamName())){
            addFlux();
            this.oNode.log(new LogEntry("This Overlay Node isn't streaming: " + this.sr.getStreamName()));
            // Send the streaming request to the next node in the path
            try{
                this.oNode.log(new LogEntry("Resending the streaming request along the path!"));
                PathNode nextNode = this.sr.getPath().getNext(this.oNode.getId());
                Socket s = new Socket(nextNode.getNodeIPAddress().toString(), Util.PORT);
                TCPConnection nextC = new TCPConnection(s);
                byte[] srBytes = sr.serialize();
                nextC.send(2, srBytes); // Send the request to the next node in the path

                // Receive and send the video metadata info
                //Packet p = nextC.receive();
                //this.c.send(p);
                //this.oNode.log(new LogEntry("Received and sent video metadata!"));

                // Receive and send end of stream notification
                Packet endOfStreamNotification = nextC.receive();
                this.c.send(endOfStreamNotification);
                this.oNode.log(new LogEntry("Received end of stream notification!"));
                this.oNode.stopStreaming(this.sr.getStreamName());

                this.c.stopConnection();
            }catch(Exception e){
                e.printStackTrace();
            }   
        }else{
            addFlux();
            this.oNode.log(new LogEntry("This Overlay Node is already streaming: " + this.sr.getStreamName()));;
            try{
                //this.oNode.log(new LogEntry("Sending video metadata!"));
                //VideoMetadata vm = new VideoMetadata(100, this.sr.getStreamName());
                //byte[] vmBytes = vm.serialize();
                //this.c.send(6, vmBytes); // Send the request to the next node in the path
                this.c.stopConnection();
            }catch(Exception e){
                e.printStackTrace();
            } 
        }
    }
}

