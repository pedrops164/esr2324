package Common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ServerStreams {
    private int serverID;
    private String serverIP;
    private List<String> streams;

    public ServerStreams(List<String> streams, int sid, String sip){
        this.streams = streams;
        this.serverID = sid;
        this.serverIP = sip;
    }

    public void serialize (DataOutputStream out){
        try{
            out.writeInt(serverID);
            out.writeUTF(serverIP);
            out.writeInt(streams.size());
            for(String s : this.streams){
                out.writeUTF(s);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static ServerStreams deserialize (DataInputStream in){
        int size = 0;
        String serverIP = null;
        List<String> streams = new ArrayList<>();
        int serverID = 0;

        try{
            serverID = in.readInt();
            serverIP = in.readUTF();
            size = in.readInt();
            for(int i=0; i<size; i++){
                streams.add(in.readUTF());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ServerStreams(streams, serverID, serverIP);
    }

    public int getID(){
        return this.serverID;
    }

    public String getIP(){
        return this.serverIP;
    }

    public List<String> getStreams(){
        return this.streams;
    }
}   
