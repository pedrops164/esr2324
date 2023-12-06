package Rendezvous_Point;

import Common.*;
import Common.TCPConnection.Packet;

import java.io.IOException;
import java.net.*;

public class RPHandlerTCP implements Runnable {
    private RP rp;
    private ServerSocket ss;
    private boolean running;

    public RPHandlerTCP(RP rp) {
        this.rp = rp;

        try{
            this.ss = new ServerSocket(Util.PORT); // socket that receives TCP packets
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Listens to new requests sent to the RP
        try{
            this.running = true;
            rp.log(new LogEntry("Now Listening to TCP requests"));
            while(this.running){
                Socket s = this.ss.accept();
                TCPConnection c = new TCPConnection(s);
                Packet p = c.receive();
                Thread t;

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    case 1: // New available stream in a server
                        rp.log(new LogEntry("Received available streams warning from server " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new HandleServerStreams(c, p, rp));
                        t.start();
                        break;
                    case 2: // Video stream request
                        rp.log(new LogEntry("Received video stream request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new HandleStreamRequests(c, p, rp));
                        t.start();
                        break;
                    case 3: // Client requests the available streams
                        rp.log(new LogEntry("Received available stream request from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new HandleNotifyStreams(c, rp, s.getInetAddress().getHostAddress()));
                        t.start();
                        break;
                    case 4: // Topology changes or removed message  from Bootstrapper 
                        String msg = new String(p.data);
                        if (msg.equals("REMOVED"))
                        {
                            c.send(4, "OK".getBytes());
                            this.rp.log(new LogEntry("Received REMOVED message from bootstrapper. Turning off..."));
                            this.rp.turnOff();
                        }
                        else
                        {
                            this.rp.log(new LogEntry("Received topology changes message from Bootstrapper"));
                            t = new Thread(new TopologyChangesWorker(this.rp, c));
                            t.start();
                        }
                        break;
                    case 5: // Client Flood Message
                        rp.log(new LogEntry("Received flood message from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new RPFloodWorker(rp, p));
                        t.start();
                        break;
                    case 7: // LIVENESS CHECK message
                        //rp.log(new LogEntry("Received liveness check from " + s.getInetAddress().getHostAddress()));
                        t = new Thread(new RPLivenessCheckWorker(rp, c));
                        t.start();
                        break;
                    case 8: // EOS notification
                        t = new Thread(new HandleEndOfStream(rp, p));
                        t.start();
                        break;
                    case 9: // Stop streaming request
                        t = new Thread(new HandleStopStreaming(rp, p));
                        t.start();
                        break;
                    default:
                        rp.log(new LogEntry("Packet type not recognized. Message ignored! Type: " + p.type ));
                        c.stopConnection();
                        break;
                }
            }
        }
        catch (SocketException e)
        {
            this.rp.log(new LogEntry("Turning off TCP handler"));
            return;
        }
        catch(Exception e)
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