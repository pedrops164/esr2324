package Common;

import java.io.FileInputStream;

public class Video{
    private String videoName;
    private FileInputStream fis;
    private int currentFrame;

    public Video(String name){
        try{
            this.videoName = name;
            this.fis = new FileInputStream(name);
            this.currentFrame = -1;
        }catch(Exception e){
            e.printStackTrace();
        }
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

    public int getFrameNumber() {
        return this.currentFrame;
    }
}