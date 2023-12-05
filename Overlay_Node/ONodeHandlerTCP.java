package Overlay_Node;

import java.util.*;
import java.net.*;
import Common.*;
import Common.TCPConnection.Packet;

class ONodeHandlerTCP implements Runnable
{
    private ServerSocket ss;
    private ONode oNode;
    
    public ONodeHandlerTCP(ONode node)
    {
        this.oNode = node;
        
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
            this.oNode.log(new LogEntry("Now Listening to TCP requests"));
            while(true)
            {
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;
                String address = s.getInetAddress().getHostAddress();
                
                switch(p.type)
                {   
                    case 2: // New stream request from a client
                        this.oNode.log(new LogEntry("New streaming request!"));
                        t = new Thread(new HandleStreamingRequest(this.oNode, p, c));
                        t.start();
                        break;
                    case 4:
                        if (this.oNode.isBoostrapper())
                        {
                            this.oNode.log(new LogEntry("Bootstrapper : Received neighbours request from " + address));
                            t = new Thread(new BootsrapperWorker(oNode, this.oNode.getBootstrapperHandler(), c, address));
                            t.start();
                        }
                        else
                        {
                            this.oNode.log(new LogEntry("Received topology changes message from Bootstrapper"));
                            t = new Thread(new TopologyChangesWorker(this.oNode, c));
                            t.start();
                        }
                        break;
                    case 5: // Flood Message 
                        this.oNode.log(new LogEntry("Received flood message from " + address));
                        t = new Thread(new NormalFloodWorker(this.oNode, p));    
                        t.start();
                        break;
                    case 7: // ALIVE? message
                        //this.oNode.log(new LogEntry("Received liveness check from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new LivenessCheckWorker(this.oNode, c, p));
                        t.start();
                        break;
                    case 8: // End of stream notification
                        t = new Thread(new HandleEndOfStream(this.oNode, p));
                        t.start();
                        break;
                    default:
                        this.oNode.log(new LogEntry("Packet type < " + p.type + " > not recognized. Message ignored!"));
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

