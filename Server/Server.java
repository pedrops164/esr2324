package Server;
import Common.NeighbourReader;
import Common.ServerStreams;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

public class Server {
    private int id;
    private String RPIP;
    private Map<Integer, String> neighbours;

    private List<String> streams;

    public Server(String []args, NeighbourReader nr){
        this.id = Integer.parseInt(args[0]);
        this.neighbours = nr.readNeighbours();
        this.RPIP = nr.getRPString();
        this.streams = new ArrayList<>();
        
        for(int i=2; i<args.length; i++){
            File path = new File(args[i]);
            File [] listOfFiles = path.listFiles();
            for(File f : listOfFiles){
                if(f.isFile()){
                    this.streams.add(f.getName());
                }
            }
        }
    }

    // Notify the RP about the available streams in this Server
    public void notifyStreamsRP(){
        try{    
            // Send request
            ServerStreams sstreams = new ServerStreams(this.streams, this.id);
            Socket s = new Socket(this.RPIP, 333);
            TCPConnection c = new TCPConnection(s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            sstreams.serialize(out);
            out.flush();
            byte [] data = baos.toByteArray();
            c.send(1, data); // Send the request to the RP

            // Answer to the request
            Packet p = c.receive();
            ByteArrayInputStream bais = new ByteArrayInputStream(p.data);
            DataInputStream in = new DataInputStream(bais);
            String resp = in.readUTF();
            System.out.println("RP response: " +  resp);
        }catch(Exception e){
            e.printStackTrace();
        }
    }   

    public void availableStreams(){
        System.out.println("Available Streams:");
        for(String stream : this.streams){
            System.out.println(stream);
        }
    }

    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        Server server = new Server(args, nr);

        // Tell the available streams to the RP
        server.notifyStreamsRP();
    }
}
