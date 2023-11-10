package Server;

import Common.NeighbourReader;
import Common.ServerStreams;
import Common.StreamRequest;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private int id;
    private String ip;
    private String RPIP;
    private Map<Integer, String> neighbours;
    private ServerSocket ss;

    private List<String> streams;

    public Server(String []args, NeighbourReader nr){
        this.id = Integer.parseInt(args[0]);
        this.neighbours = nr.readNeighbours();
        this.RPIP = nr.getRPString();
        this.streams = new ArrayList<>();
        this.ip = this.neighbours.get(this.id);

        try{
            this.ss = new ServerSocket(1234);
        }catch(Exception e){
            e.printStackTrace();
        }

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
            ServerStreams sstreams = new ServerStreams(this.streams, this.id, this.ip);
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

    // Listens to new requests sent to the RP
    public void listen(){
        while(true){
            try{
                System.out.println("Server waiting for new requests!");
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    case 2: // New video stream request
                        t = new Thread(new ServerWorker1(c, p));
                        t.start();
                        break;
                    default:
                        System.out.println("Packet type not recognized. Message ignored!");
                        c.stopConnection();
                        break;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
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
        server.listen(); // Listen to new TCP requests
    }
}

// Responsible to handle new resquests of video streaming!
class ServerWorker1 implements Runnable{
    private TCPConnection connection;
    private Packet receivedPacket;

    public ServerWorker1(TCPConnection c, Packet p){
        this.connection = c;
        this.receivedPacket = p;
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        StreamRequest sr = StreamRequest.deserialize(in);
        
        System.out.println("The RP wants me to start streaming: " + sr.getStreamName() + "!");
        // End TCP connection
        this.connection.stopConnection();

        // Start the UDP video streaming. (Send directly to the RP)

    }    
}
