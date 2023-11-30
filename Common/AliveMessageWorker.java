package Common;

import java.io.IOException;

import Common.TCPConnection.Packet;

public class AliveMessageWorker implements Runnable {
    private Node node;
    private TCPConnection c;
    private String address;

    public AliveMessageWorker(Node node, TCPConnection c, String address)
    {
        this.node = node;
        this.c = c;
        this.address = address;
    }

    @Override
    public void run() 
    {
        Packet p = new Packet(7, "OK".getBytes());
        c.send(p);
        
        try 
        {
            this.node.log(new LogEntry("Sent packet: " + p + " to " + address));
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }
}
