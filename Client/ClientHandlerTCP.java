package Client;

import java.util.*;
import java.io.IOException;
import java.net.*;
import Common.*;
import Common.TCPConnection.Packet;

public class ClientHandlerTCP implements Runnable {
    private ServerSocket ss;
    private Client client;
    private boolean running;
    
    public ClientHandlerTCP(Client client)
    {
        this.client = client;
        
        try 
        {
            this.ss = new ServerSocket(Util.PORT);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();    
        }
    }
    
    @Override
    public void run() 
    {
        try
        {   
            this.client.log(new LogEntry("Now Listening to TCP messages"));
            this.running = true;
            while(this.running)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t = null;
                
                switch(p.type)
                {
                    case 4: // Topology changes or removed message  from Bootstrapper 
                        String msg = new String(p.data);
                        if (msg.equals("REMOVED"))
                        {
                            c.send(4, "OK".getBytes());
                            this.client.log(new LogEntry("Received REMOVED message from bootstrapper. Turning off..."));
                            this.client.turnOff();
                        }
                        else
                        {
                            this.client.log(new LogEntry("Received topology changes message from Bootstrapper"));
                            t = new Thread(new TopologyChangesWorker(this.client, c));
                            t.start();
                        }
                        break;
                    case 5: // Flood Message from client
                    this.client.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new NormalFloodWorker(client, p));    
                        t.start();
                        break;
                    case 6: // Flood Response from RP
                        this.client.log(new LogEntry("Received flood response from RP: " + s.getInetAddress().getHostAddress()));
                        client.receivePath(p);
                        break;
                    case 7: // ALIVE? message
                        //this.oNode.log(new LogEntry("Received liveness check from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new LivenessCheckWorker(this.client, c, p));
                        t.start();
                        break;
                    case 8: // End of stream notification
                        t = new Thread(new HandleEndOfStream(client, p));
                        t.start();
                        break;
                    default:
                        this.client.log(new LogEntry("Packet type not recognized. Message ignored!"));
                        c.stopConnection();
                        break;
                }
            }
            
        } catch (SocketException e){
            this.client.log(new LogEntry("Turning off TCP handler"));
            return;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void turnOff()
    {
        this.running = false;
        try {
            this.ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}