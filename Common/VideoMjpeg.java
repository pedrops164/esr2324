package Common;

import java.awt.Toolkit;
import java.awt.Image;

public class VideoMjpeg extends Video {
    private Toolkit toolkit;

    public VideoMjpeg(String videoName) {
        super(videoName);
        // We cant get the value of fps from a MJpeg video, so we just set an arbitrary value in the frame period
        // Frame period is the inverse of fps (frames per second)
        this.toolkit = Toolkit.getDefaultToolkit();
        this.framePeriod = 25;
    }

    // Returns the bytes of the next frame, or returns null if all frames have been read
    public byte[] getNextVideoFrame(){
        byte[] frame_length = new byte[5];
        int length = 0;
        String length_string;
        byte[] res = null;
        try{
            //read current frame length
            this.fis.read(frame_length,0,5);
            
            //transform frame_length to integer
            length_string = new String(frame_length);
            length = Integer.parseInt(length_string);
            
            res = new byte[length];
            int bytes_read = this.fis.read(res,0,length);
            this.currentFrame++;
            if (bytes_read == -1) {
                // Reached end of file
                return null;
            }
        } catch(Exception e){
            return null;
        }
        return res;
    }

    public static Image decode(UDPDatagram udpDatagram) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        byte[] payload = udpDatagram.getPayload();
        int payload_length = payload.length;
        //get an Image object from the payload bitstream
        Image image = toolkit.createImage(payload, 0, payload_length);
        return image;        
    }

}