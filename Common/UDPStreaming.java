package Common;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/*
* Class used to handle all UDP communications
*/

public class UDPStreaming {
    private DatagramSocket s;
    private InetAddress destIP;
    private int port;

    private Video video;
    private int videoType;
    public int frame_period; 
    private int frame_nb;
    private int videoLength;
    // private byte[] buffer;

    // Sending constructor
    public UDPStreaming(DatagramSocket s, String dest, int port, String videoPath, 
                        int videoType, int frame_period, int frame_nb, int vl){
        try{
            this.s = s;
            this.destIP = InetAddress.getByName(dest);
            this.port = port;
            this.video = new Video(videoPath);
            this.videoType = videoType;
            this.frame_period = frame_period;
            this.frame_nb = frame_nb;
            this.videoLength = vl;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Receiving Constructor
    public UDPStreaming(DatagramSocket s, int port){
        this.s = s;
        this.port = port;
    }

    // Method responsible for sending the next frame to the destination
    // We can make this method loop the video instead on stop sending when the video in done
    public boolean sendNextFrame(){
        if(this.frame_nb < this.videoLength){
            // Get video frame
            this.frame_nb++;
            byte[] buffer = this.video.getNextVideoFrame();

            // Create UDPDatagram Object
            UDPDatagram datagramObj = new UDPDatagram(this.videoType, this.frame_nb, this.frame_period * this.frame_nb, buffer, buffer.length, this.frame_period, this.video.getVideoName());
            
            // Get bytes for the datagram
            byte[] bytes = datagramObj.serialize();

            // Construct sending datagram and send it
            DatagramPacket datapackt = new DatagramPacket(bytes, bytes.length, this.destIP, this.port);
	        try{
                this.s.send(datapackt);
            }catch(Exception e){
                e.printStackTrace();
            }
            return true;
        }
        // If the frame wasn't sent we return false
        return false;
    }

    // Method responsible to receive a frame of video (UDPDatagram)
    public UDPDatagram receiveFrame(){
        UDPDatagram recvUdpDatagram = null;
        try{
            // Initialize receiving buffer
            byte[] recvBuffer = new byte[15000];
            // Construct receiving Datagram
            DatagramPacket recvDatagramPacket = new DatagramPacket(recvBuffer, recvBuffer.length);

            // Receive the Datagram
            this.s.receive(recvDatagramPacket);

            // Get the receiving bytes and construct the UDPDatragram
            byte[] recvBytes = recvDatagramPacket.getData();
            recvUdpDatagram = UDPDatagram.deserialize(recvBytes);
        }catch(Exception e){
            e.printStackTrace();
        }
        return recvUdpDatagram;
    }

    public int getFrameNumber(){
        return this.frame_nb;
    }
}   