package Common;

import java.io.*;

/* 
 * This class represents the content of a new video stream request (TCP).
*/

public class StreamRequest implements Serializable {
    private String streamName;
    private int clientID; // Acho que o clientID não vai ser necessário
    private Path path;
    // Flag that represents whether or not this is a stream request to fix a path in case of a faild node
    private boolean fixPath;

    public StreamRequest(String streamName, int id, Path path, boolean fixPath){
        this.streamName = streamName;
        this.clientID = id;
        this.path = path;
        this.fixPath = fixPath;
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

    public boolean fixingPath() {
        return this.fixPath;
    }
}
