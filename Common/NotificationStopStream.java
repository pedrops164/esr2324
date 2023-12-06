package Common;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;

/*
 * Class responsible for 2 things:
 *  - End the client doesn't want to keep seeing the stream 
 *    he warns every node in the path to the RP to end the stream
 * 
 *  - When on the in the path from the client to the RP fails, the client warns
 *    every other node in the path for the streaming to be stopped and started in
 *    another path
 */


public class NotificationStopStream implements Serializable {
    private String streamName;
    private Path pathToRP;
    private boolean endStream;

    public NotificationStopStream(String streamName, Path pathToRP, boolean endStream) {
        this.streamName = streamName;
        this.pathToRP = pathToRP;
        this.endStream = endStream;
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

    public boolean getEndStream(){
        return this.endStream;
    }
}