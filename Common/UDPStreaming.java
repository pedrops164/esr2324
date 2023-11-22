package Common;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/*
 * Class used to handle all the UDP communications
 */

public class UDPStreaming {
    private static int frame_period = 100; // 100 ms each frame takes on the screen
    private DatagramSocket s;
    private InetAddress destIP;
    private int port;

    private Video video;
    private int videoType;
    private int frame_nb;
    private byte[] buffer;

    public UDPStreaming(DatagramSocket s, String dest, int port, String videoName, int videoType, int frame_nb){
        try{
            this.s = s;
            this.destIP = InetAddress.getByName(dest);
            this.port = port;
            this.video = new Video(videoName);
            this.videoType = videoType;
            this.frame_nb = frame_nb;
            this.buffer = new byte[15000];
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    // Method responsible for sending the next frame to the destination
    public void sendNextFrame(){
        this.frame_nb++;
        int size = this.video.getNextVideoFrame(this.buffer);
        UDPDatagram datagramObj = new UDPDatagram(this.videoType, this.frame_nb, frame_period * this.frame_nb,this.buffer, size);
        
        size = datagramObj.datagramSize();        
        byte[] datagram = new byte[size]; 
        datagramObj.getDatagram(datagram);
        DatagramPacket datapackt = new DatagramPacket(datagram, size, this.destIP, this.port);
	    try{
            this.s.send(datapackt);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Method responsible to receive a frame of video (UDPDatagram)
    public void receiveFrame(){

    }
}   
