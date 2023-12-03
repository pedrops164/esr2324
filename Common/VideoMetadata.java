package Common;

import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Serializable;

public class VideoMetadata implements Serializable {
    private int framePeriod;
    private String streamName;

    public VideoMetadata(int framePeriod, String streamName) {
        this.framePeriod = framePeriod;
        this.streamName = streamName;
    }

    public static VideoMetadata deserialize(byte[] receivedBytes) {
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
            ObjectInput in = new ObjectInputStream(bais);

            VideoMetadata vmd = (VideoMetadata) in.readObject();
            return vmd;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(this);
            byte b[] = baos.toByteArray();
            out.close();
            baos.close();
            return b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getFramePeriod() {
        return this.framePeriod;
    }

    public String getStreamName() {
        return this.streamName;
    }

    public String toString(){
        return "Video Metadata:\nFrame Period- " + this.framePeriod + "\nStreamName- " + this.streamName;
    }
}