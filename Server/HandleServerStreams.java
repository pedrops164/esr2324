package Server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import Common.LogEntry;
import Common.ServerStreams;
import Common.TCPConnection;

/*
* This class is responsible to answer the RP with ServerStreams for the RP to update it's server rankings.
*/

public class HandleServerStreams implements Runnable{
    private Server server;
    private TCPConnection c;

    public HandleServerStreams(Server s, TCPConnection c){
        this.server = s;
        this.c = c;
    }

    public void run(){
        while(true){
            try{
                this.server.log(new LogEntry("Got a new ServerStream request from the RP!"));
                // Send the ServerStream to the RP
                ServerStreams sstreams = new ServerStreams(this.server.getStreams(), this.server.getId(), this.server.getIps().get(0));  
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                sstreams.serialize(out);
                out.flush();
                byte [] data = baos.toByteArray();
                c.send(1, data); // Send the request to the RP
                this.server.log(new LogEntry("Notifying Rendezvous Point about the available streams"));
                
                // Wait for the next request 
                this.c.receive();
            }catch(Exception e){
                e.printStackTrace();
            }   
        }
    }
}
