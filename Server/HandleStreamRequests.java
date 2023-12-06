package Server;

import Common.LogEntry;
import Common.StreamRequest;
import Common.UDPStreaming;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Util;

import java.net.*;

// Responsible to handle new requests of video streaming!
class HandleStreamRequests implements Runnable{
    private TCPConnection connection;
    private Packet receivedPacket;
    private String RPIP;
    private DatagramSocket ds;
    private Server server;
    private boolean sending;

    public HandleStreamRequests(TCPConnection connection, Packet p, String RPIP, Server server){
        this.connection = connection;
        this.receivedPacket = p;
        this.RPIP = RPIP;
        this.server = server;
        this.sending = true;

        try {
            this.ds = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return ""; // No extension found
        }
        return fileName.substring(lastIndexOfDot + 1);
    }

    public void setSending(boolean sending){
        this.sending = sending;
    }

    public void run(){

        // Receive request
        byte[] data = this.receivedPacket.data;
        StreamRequest sr = StreamRequest.deserialize(data);
        String streamName = sr.getStreamName();

        // Add streaming worker to the Server
        this.server.addStreamingWorker(streamName ,this);

        String extension = getExtension(streamName);
        
        UDPStreaming streaming = new UDPStreaming(this.ds, this.RPIP, Util.PORT, this.server.getStreamsDir() + streamName, extension, 0);
        int frame_period = streaming.getFramePeriod();

        // Start the UDP video streaming. (Send directly to the RP)
        try {
            this.server.log(new LogEntry("Streaming '" + streamName + "' through UDP!"));
            
            while (sending) {
                sending = streaming.sendNextFrame();
                // Wait for 'frame_period' milliseconds before sending the next packet
                Thread.sleep(frame_period);
            }
            this.server.log(new LogEntry("Sent " + streaming.getFrameNumber() + " frames!"));
            // notify the RP that the stream has ended
            this.server.notifyEndOfStream(streamName);
            // End the TPC connection
            this.connection.stopConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
}