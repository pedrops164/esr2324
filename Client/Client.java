package Client;

import Common.NeighbourReader;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client {
    private int id;
    private String RPIP;
    private Map<Integer, String> neighbours;

    private List<String> availableStreams;

    public Client(String args[], NeighbourReader nr){
        this.id = Integer.parseInt(args[0]);
        this.neighbours = nr.readNeighbours(); 
        this.RPIP = nr.getRPString();
        this.availableStreams = new ArrayList<>();
    }

    public void getAvailableStreams(){
        try{
            // Send the request
            Socket s = new Socket(this.RPIP, 333);
            TCPConnection c = new TCPConnection(s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Available streams.");
            out.flush();
            byte [] data = baos.toByteArray();
            c.send(3, data); // Send the request to the RP

            // Answer to the request
            Packet p = c.receive();
            ByteArrayInputStream bais = new ByteArrayInputStream(p.data);
            DataInputStream in = new DataInputStream(bais);
            int nr = in.readInt();
            for(int i=0; i<nr; i++){
                this.availableStreams.add(in.readUTF());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public void showAvailableStreams(){
        int i = 1;
        System.out.println("Available streams to watch:");
        for(String stream : this.availableStreams){
            System.out.println("Stream " + Integer.toString(i) + ": " + stream);
            i++;
        }
        System.out.println("Select one to watch by inputing it's number:");
    }

    public void requestStreaming(int streamId){
        try{
            // Send request
            String stream = this.availableStreams.get(streamId-1);
            StreamRequest sr = new StreamRequest(stream, this.id);
            Socket s = new Socket(this.RPIP, 333);
            TCPConnection c = new TCPConnection(s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            sr.serialize(out);
            out.flush();
            byte [] data = baos.toByteArray();
            c.send(2, data); // Send the request to the RP
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        Client c = new Client(args, nr);
        c.getAvailableStreams();
        c.showAvailableStreams();
        Scanner in = new Scanner(System.in);
        int streamId = in.nextInt();
        c.requestStreaming(streamId);
        in.close();
    }
}   
