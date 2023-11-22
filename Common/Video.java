package Common;

import java.io.FileInputStream;

public class Video{
    private String videoName;
    private FileInputStream fis;

    public Video(String name){
        try{
            this.videoName = name;
            this.fis = new FileInputStream(name);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public int getNextVideoFrame(byte[] frame){
        int length = 0, res = 0;
        String length_string;
        byte[] frame_length = new byte[5];
        try{
            //read current frame length
            this.fis.read(frame_length,0,5);
            
            //transform frame_length to integer
            length_string = new String(frame_length);
            length = Integer.parseInt(length_string);
            
            res = this.fis.read(frame,0,length); 
        }catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }
}