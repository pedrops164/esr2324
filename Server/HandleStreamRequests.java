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

    public HandleStreamRequests(TCPConnection connection, Packet p, String RPIP, Server server){
        this.connection = connection;
        this.receivedPacket = p;
        this.RPIP = RPIP;
        this.server = server;
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

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        StreamRequest sr = StreamRequest.deserialize(data);
        String streamName = sr.getStreamName();

        // get video attributes....
        int videoLength = 100; // number of frames
        int frame_period = 25; // time between frames in ms.
        int video_extension = 26; //26 is Mjpeg type

        UDPStreaming streaming = new UDPStreaming(this.ds, this.RPIP, Util.PORT, this.server.getStreamsDir() + streamName, video_extension, frame_period, 0, videoLength);
        // Start the UDP video streaming. (Send directly to the RP)
        try {
            this.server.log(new LogEntry("Streaming '" + streamName + "' through UDP!"));
            boolean sent = true;
            while (sent) {
                sent = streaming.sendNextFrame();
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


//public void run(){
//        // Receive request
//        byte[] data = this.receivedPacket.data;
//        StreamRequest sr = StreamRequest.deserialize(data);
//
//        //Deserialize the Path
//
//        String streamName = sr.getStreamName();
//
//        // get video attributes....
//        int videoLength = 100; // number of frames
//        int frame_period = 25; // time between frames in ms.
//        int video_extension = 26; //26 is Mjpeg type
//
//        //VideoMetadata vmd = new VideoMetadata(frame_period, streamName);
//        // Convert VideoMetadata to bytes (serialize) and send the packet to RP (type 6 represents video metadata)
//        //Packet packetMetadata = new Packet(6, vmd.serialize());
//        //this.connection.send(packetMetadata);
//
//        // Start the UDP video streaming. (Send directly to the RP)
//        try {
//            this.server.log(new LogEntry("Streaming '" + streamName + "' through UDP!"));
//            Video video = new Video(this.server.getStreamsDir() + streamName);
//            byte[] videoBuffer = null;
//            while ((videoBuffer = video.getNextVideoFrame()) != null) {
//                // Get the next frame of the video
//
//                int frameNumber = video.getFrameNumber();
//                //Builds a UDPDatagram object containing the frame
//	            UDPDatagram udpDatagram = new UDPDatagram(video_extension, frameNumber, frameNumber*frame_period,
//                 videoBuffer, videoBuffer.length, frame_period, streamName);
//                
//                // get the bytes of the UDPDatagram
//                byte[] packetBytes = udpDatagram.serialize();
//
//	            // Initialize the DatagramPacket and send it over the UDP socket 
//	            DatagramPacket senddp = new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName(this.RPIP), Util.PORT);
//	            this.ds.send(senddp);
//
//                // Wait for 'frame_period' milliseconds before sending the next packet
//                Thread.sleep(frame_period);
//            }
//            this.server.log(new LogEntry("Sent " + video.getFrameNumber() + " frames!"));
//            // notify the RP that the stream has ended
//            this.server.notifyEndOfStream(streamName);
//
//            // Packet streamEndedNotification = new Packet(8);
//            // Close the socket
//            // this.connection.send(streamEndedNotification);
//            this.connection.stopConnection();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }    
