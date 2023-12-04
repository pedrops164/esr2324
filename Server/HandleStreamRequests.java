package Server;

import Common.LogEntry;
import Common.StreamRequest;
import Common.UDPDatagram;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Video;
import Common.VideoMjpeg;
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

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        StreamRequest sr = StreamRequest.deserialize(data);

        //Deserialize the Path

        String streamName = sr.getStreamName();


        //VideoMetadata vmd = new VideoMetadata(frame_period, streamName);
        // Convert VideoMetadata to bytes (serialize) and send the packet to RP (type 6 represents video metadata)
        //Packet packetMetadata = new Packet(6, vmd.serialize());
        //this.connection.send(packetMetadata);

        // Start the UDP video streaming. (Send directly to the RP)
        try {
            //this.server.log(new LogEntry("Sent VideoMetadata packet to RP"));
            this.server.log(new LogEntry("Streaming '" + streamName + "' through UDP!"));
            VideoMjpeg video = new VideoMjpeg(this.server.getStreamsDir() + streamName);
            
            // get video attributes....
            int frame_period = video.getFramePeriod(); // time between frames in ms.
            int video_extension = 26; //26 is Mjpeg type

            byte[] videoBuffer = null;
            while ((videoBuffer = video.getNextVideoFrame()) != null) {
                // Get the next frame of the video

                int frameNumber = video.getFrameNumber();
                //Builds a UDPDatagram object containing the frame
	            UDPDatagram udpDatagram = new UDPDatagram(video_extension, frameNumber, frameNumber*frame_period,
                 videoBuffer, videoBuffer.length, frame_period, streamName);
                
                // get the bytes of the UDPDatagram
                byte[] packetBytes = udpDatagram.serialize();

	            // Initialize the DatagramPacket and send it over the UDP socket 
	            DatagramPacket senddp = new DatagramPacket(packetBytes, packetBytes.length, InetAddress.getByName(this.RPIP), Util.PORT);
	            this.ds.send(senddp);

                // Wait for 'frame_period' milliseconds before sending the next packet
                Thread.sleep(frame_period);
            }
            this.server.log(new LogEntry("Sent " + video.getFrameNumber() + " frames!"));
            // notify the RP that the stream has ended
            this.server.notifyEndOfStream(streamName);

            //Packet streamEndedNotification = new Packet(8);
            // Close the socket
            //this.connection.send(streamEndedNotification);
            this.connection.stopConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
    
    public static String getExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return ""; // No extension found
        }
        return fileName.substring(lastIndexOfDot + 1);
    }
}
