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
    private int frame_nb;
    // private byte[] buffer;

    // Sending constructor
    public UDPStreaming(DatagramSocket s, String dest, int port, String videoPath, 
                        String videoExtension, int frame_nb){
        try{
            this.s = s;
            this.destIP = InetAddress.getByName(dest);
            this.port = port;
            if(videoExtension.equals("Mjpeg")){
                this.videoType = 26;
                this.video = new VideoMjpeg(videoPath);
            }else if(videoExtension.equals("MP4")){
                this.videoType = 26;
                this.video = new VideoMP4(videoPath);
            }else{
                System.out.println("Extensão de vídeo inválida!");
            }
            this.frame_nb = frame_nb;
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
        // Get video frame
        this.frame_nb++;
        byte[] buffer = this.video.getNextVideoFrame();
        // Return false if there are no more frames
        if(buffer == null){
            return false;
        }  

        // Create UDPDatagram Object
        UDPDatagram datagramObj = new UDPDatagram(this.videoType, this.frame_nb, this.video.getFramePeriod() * this.frame_nb, buffer, buffer.length, this.video.getFramePeriod(), this.video.getVideoName());
            
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

    public int getFramePeriod(){
        return this.video.getFramePeriod();
    }
}   