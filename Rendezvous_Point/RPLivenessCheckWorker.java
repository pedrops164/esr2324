package Rendezvous_Point;

import Common.LogEntry;
import Common.Node;
import Common.TCPConnection;

public class RPLivenessCheckWorker implements Runnable {

    private Node node;
    private TCPConnection c;

    public RPLivenessCheckWorker(Node node, TCPConnection c)
    {
        this.node = node;
        this.c = c;
    }

    @Override
    public void run() 
    {
        try 
        {
            byte[] msg = new byte[1];
            msg[0] = (byte)1;
            this.c.send(7, msg);
            this.c.stopConnection();
            this.node.log(new LogEntry("Sent liveness check answer saying path is alive back." + (msg)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
}
