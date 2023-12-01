package Common;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/* 
 * This class represents the content of a new video stream request (TCP).
*/

import java.io.Serializable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;

public class StreamRequest implements Serializable {
    private String streamName;
    private int clientID;   
    private Path path;

    public StreamRequest(String streamName, int id, Path path){
        this.streamName = streamName;
        this.clientID = id;
        this.path = path;
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

    public static StreamRequest deserialize(byte[] receivedBytes) {
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
            ObjectInput in = new ObjectInputStream(bais);

            StreamRequest ret = (StreamRequest) in.readObject();
            return ret;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public int getClientID(){
        return this.clientID;
    }

    public String getStreamName(){
        return this.streamName;
    }

    public Path getPath() {
        return this.path;
    }
}
