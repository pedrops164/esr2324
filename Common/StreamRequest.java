package Common;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/* 
 * This class represents the content of a new video stream request (TCP).
*/

public class StreamRequest {
    private String streamName;
    private int clientID;   

    public StreamRequest(String streamName, int id){
        this.streamName = streamName;
        this.clientID = id;
    }

    public void serialize (DataOutputStream out){
        try{
            out.writeInt(this.clientID);
            out.writeUTF(this.streamName);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static StreamRequest deserialize (DataInputStream in){
        String streamName = null;
        int clientID = 0;
        try{
            clientID = in.readInt();
            streamName = in.readUTF();
        }catch (Exception e){
            e.printStackTrace();
        }
        return new StreamRequest(streamName, clientID);
    }

    public int getClientID(){
        return this.clientID;
    }

    public String getStreamName(){
        return this.streamName;
    }
}
