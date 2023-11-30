package Rendezvous_Point;

import Common.TCPConnection;
import Common.LogEntry;

import java.io.*;
import java.util.*;

// Responsible to notify the client of the available streams
// RPWorker3
class HandleNotifyStreams implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private String clientIP;

    public HandleNotifyStreams(TCPConnection c, RP rp, String clientIP){
        this.rp = rp;
        this.connection = c;
        this.clientIP = clientIP;
    }

    public void run(){
        List<String> streams = rp.getAvailableStreams();

        // Answer
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeInt(streams.size());
            for(String stream : streams){
                out.writeUTF(stream);
            }
            out.flush();
            this.connection.send(3, baos.toByteArray());
            this.rp.log(new LogEntry("Sent available streams to client " + this.clientIP));
        }catch(Exception e){
            e.printStackTrace();
        }

        // End TCP connection
        this.connection.stopConnection();
    }    
}