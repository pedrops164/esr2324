package Common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ServerStreams {
    private int serverID;
    private List<String> streams;

    public ServerStreams(List<String> streams, int sid){
        this.streams = streams;
        this.serverID = sid;
    }

    public void serialize (DataOutputStream out){
        try{
            out.writeInt(serverID);
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
        List<String> streams = new ArrayList<>();
        int serverID = 0;
        try{
            serverID = in.readInt();
            size = in.readInt();
            for(int i=0; i<size; i++){
                streams.add(in.readUTF());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ServerStreams(streams, serverID);
    }

    public int getID(){
        return this.serverID;
    }

    public List<String> getStreams(){
        return this.streams;
    }
}   
