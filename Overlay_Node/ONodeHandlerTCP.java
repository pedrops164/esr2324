package Overlay_Node;

import java.util.*;
import java.io.IOException;
import java.net.*;
import Common.*;
import Common.TCPConnection.Packet;

class ONodeHandlerTCP implements Runnable
{
    private ServerSocket ss;
    private ONode oNode;
    private boolean running;
    
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
            this.running = true;
            this.oNode.log(new LogEntry("Now Listening to TCP requests"));
            while(this.running)
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
                            String msg = new String(p.data);
                            if (msg.equals("REMOVED"))
                            {
                                c.send(4, "OK".getBytes());
                                this.oNode.turnOff();
                            }
                            else
                            {
                                this.oNode.log(new LogEntry("Received topology changes message from Bootstrapper"));
                                t = new Thread(new TopologyChangesWorker(this.oNode, c));
                                t.start();
                            }
                            break;
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
        catch (SocketException e){
            this.oNode.log(new LogEntry("Turning off TCP handler"));
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

