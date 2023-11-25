package Rendezvous_Point;

import Rendezvous_Point.RP;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.ServerStreams;

import java.io.*;

// Responsible to handle new requests from new streams of servers
// RPWorker1
class HandleServerStreams implements Runnable{
    private RP rp;
    private TCPConnection connection;
    private Packet receivedPacket;

    public HandleServerStreams(TCPConnection c, Packet p, RP rp){
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
        rp.addServerStreams(sstreams.getID(), sstreams.getIP(), sstreams.getStreams());

        // End TCP connection
        this.connection.stopConnection();
    }    
}