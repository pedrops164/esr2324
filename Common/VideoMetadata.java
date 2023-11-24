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

    public VideoMetadata(int framePeriod) {
        this.framePeriod = framePeriod;
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
}