package Client;

import java.util.*;
import java.net.*;
import Common.*;
import Common.TCPConnection.Packet;
import Common.Util;

public class ClientHandlerTCP implements Runnable {
    private ServerSocket ss;
    private Client client;
    
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
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t = null;
                
                switch(p.type)
                {
                    case 5: // Flood Message from client
                    this.client.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new NormalFloodWorker(client, p));    
                        t.start();
                        break;
                    case 6: // Flood Response from RP
                        this.client.log(new LogEntry("Received flood response from RP: " + s.getInetAddress().getHostAddress()));
                        client.receivePath(p);
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
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}