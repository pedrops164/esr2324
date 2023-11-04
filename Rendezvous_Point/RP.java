package Rendezvous_Point;

import Common.NeighbourReader;
import Common.ServerStreams;
import Common.TCPConnection;
import Common.TCPConnection.Packet;

import java.io.*;
import java.net.*;
import java.util.*;

public class RP{
    private int id;
    private Map<Integer, String> neighbours;
    private ServerSocket ss;
    
    // Map that associates each server id to it's available streams
    private Map<Integer, List<String>> streamServers;
    private int streamCounter;

    public RP(String args[], NeighbourReader nr){
        this.id = Integer.parseInt(args[0]);
        this.neighbours = nr.readNeighbours();
        this.streamServers = new HashMap<>();

        try{
            this.ss = new ServerSocket(333);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Listens to new requests sent to the RP
    public void listen(){
        while(true){
            try{
                System.out.print("RP waiting for new requests!");
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    case 1: // New available stream in a server
                        Thread t = new Thread(new RPWorker1(c, p, this));
                        t.start();
                        break;
                    default:
                        System.out.println("Type of packet not recognizeded!");
                        c.stopConnection();
                        break;
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void addServerStreams(int serverID, List<String> streams){
        this.streamServers.put(serverID, streams);
        System.out.println(this.streamServers);
    }
}

// Responsible to handle new requests from new streams of servers
class RPWorker1 implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;

    public RPWorker1(TCPConnection c, Packet p, RP rp){
        this.rp = rp;
        this.connection = c;
        this.receivedPacket = p;
    }

    public void run(){
        // Receive request
        byte[] data = this.receivedPacket.data;
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        ServerStreams sstreams = ServerStreams.deserialize(in);
        rp.addServerStreams(sstreams.getID(), sstreams.getStreams());

        // Answer
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Ok!");
            out.flush();
            this.connection.send(1, baos.toByteArray());
        }catch(Exception e){
            e.printStackTrace();
        }

        // End TCP connection
        this.connection.stopConnection();
    }
}