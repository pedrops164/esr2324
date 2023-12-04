package Common;

import java.io.FileInputStream;

public abstract class Video{
    protected String videoName;
    protected FileInputStream fis;
    protected int currentFrame;
    protected int framePeriod;

    public Video(String videoPath, int frame_period){
        try{
            this.videoName = videoPath.substring(videoPath.lastIndexOf("/") + 1);
            this.fis = new FileInputStream(videoPath);
            this.framePeriod = frame_period;
            this.currentFrame = -1;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public abstract byte[] getNextVideoFrame();

    public int getFrameNumber() {
        return this.currentFrame;
    }

    public String getVideoName(){
        return this.videoName;
    }
    
    public int getFramePeriod() {
        return this.framePeriod;
    }
}