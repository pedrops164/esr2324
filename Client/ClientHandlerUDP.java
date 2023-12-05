package Client;

import java.net.*;
import Common.*;


public class ClientHandlerUDP implements Runnable {
    private DatagramSocket ds;
    private Client client;
    private boolean running;
    
    public ClientHandlerUDP(Client client)
    {
        this.client = client;
        
        try {
            // open a socket for receiving UDP packets on the overlay node's port
            this.ds = new DatagramSocket(Util.PORT);
    
        } catch (Exception e) {
            e.printStackTrace();    
        }
    }

    @Override
    public void run() 
    {
        try {
            // set the buffer size
            int buffersize = 15000;
            // create the buffer to receive the packets
            byte[] receiveData = new byte[buffersize];
            // Create the packet which will receive the data
            DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);

            this.client.log(new LogEntry("Listening on UDP:" + this.client.getIps().get(0) + ":" + Util.PORT));
            this.running = true;
            while(this.running) {
                try {
                    // Receive the packet
                    this.ds.receive(receivedPacket);
                    //this.client.log(new LogEntry("Received UDP packet"));

                    // Get the received bytes from the receivedPacket
                    byte[] receivedBytes = receivedPacket.getData();

                    UDPDatagram udpDatagram = UDPDatagram.deserialize(receivedBytes);
                    this.client.log(new LogEntry("Got an UDP packet!"));
                    this.client.cvm.addFrame(udpDatagram);
                } catch (java.awt.HeadlessException e) {
                    this.client.log(new LogEntry("Must run 'export DISPLAY=:0.0' before running the client"));
                    break;
                } catch (SocketException se) {
                    this.client.log(new LogEntry("Turning off UDP handler"));
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            if (this.running)
                e.printStackTrace();  
        } finally {
            this.ds.close();
        }
    }

    public void turnOff()
    {
        this.running = false;
        this.ds.close();
    }
}