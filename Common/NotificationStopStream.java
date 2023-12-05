package Common;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;


public class NotificationStopStream implements Serializable {
    private String streamName;
    private Path pathToRP;

    public NotificationStopStream(String streamName, Path pathToRP) {
        this.streamName = streamName;
        this.pathToRP = pathToRP;
    }

    public byte[] serialize() {
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(this);
            byte b[] = baos.toByteArray();
            out.close();
            baos.close();
            return b;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static NotificationStopStream deserialize(byte[] receivedBytes) {
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
            ObjectInput in = new ObjectInputStream(bais);

            NotificationStopStream ret = (NotificationStopStream) in.readObject();
            return ret;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public String getStreamName() {
        return this.streamName;
    }

    public Path getPath() {
        return this.pathToRP;
    }
}