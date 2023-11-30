package Common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 * Class responsible to hold the information sended when Servers notify the RP about their streams 
 */

public class ServerStreams {
    private int serverID;
    private String serverIP;
    private List<String> streams;
    private LocalDateTime sendingTimeStamp; // Makes possible to calculate the best servers to stream to the RP

    // Sending constructor
    public ServerStreams(List<String> streams, int sid, String sip){
        this.streams = streams;
        this.serverID = sid;
        this.serverIP = sip;
        this.sendingTimeStamp = LocalDateTime.now();
    }

    // Receiving constructor
    public ServerStreams(List<String> streams, int sid, String sip, LocalDateTime timeStamp){
        this.streams = streams;
        this.serverID = sid;
        this.serverIP = sip;
        this.sendingTimeStamp = timeStamp;
    }

    public void serialize (DataOutputStream out){
        try{
            out.writeInt(this.serverID);
            out.writeUTF(this.serverIP);
            out.writeInt(this.sendingTimeStamp.getYear());
            out.writeInt(this.sendingTimeStamp.getMonthValue());
            out.writeInt(this.sendingTimeStamp.getDayOfMonth());
            out.writeInt(this.sendingTimeStamp.getHour());
            out.writeInt(this.sendingTimeStamp.getMinute());
            out.writeInt(this.sendingTimeStamp.getSecond());
            out.writeInt(this.sendingTimeStamp.getNano());
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
        LocalDateTime sendingTimeStamp = null;

        try{
            serverID = in.readInt();
            serverIP = in.readUTF();
            int year = in.readInt();
            int month = in.readInt();
            int day = in.readInt();
            int hour = in.readInt();
            int minute = in.readInt();
            int second = in.readInt();
            int nano = in.readInt();
            size = in.readInt();
            sendingTimeStamp = LocalDateTime.of(year, month, day, hour, minute, second, nano);
            for(int i=0; i<size; i++){
                streams.add(in.readUTF());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new ServerStreams(streams, serverID, serverIP, sendingTimeStamp);
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

    public LocalDateTime getTimeStamp(){
        return this.sendingTimeStamp;
    }
}   
