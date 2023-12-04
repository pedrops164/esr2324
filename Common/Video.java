package Common;

import java.io.FileInputStream;

public abstract class Video{
    protected String videoName;
    protected FileInputStream fis;
    protected int currentFrame;
    protected int framePeriod;

    public Video(String name){
        try{
            this.videoName = name;
            this.fis = new FileInputStream(name);
            this.currentFrame = -1;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public abstract byte[] getNextVideoFrame();

    public int getFrameNumber() {
        return this.currentFrame;
    }

    public int getFramePeriod() {
        return this.framePeriod;
    }

}