package Server;

import Common.LivenessCheckWorker;
import Common.LogEntry;
import Common.Node;
import Common.NormalFloodWorker;
import Common.ServerStreams;
import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.Util;
import Common.NotificationEOS;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Node {
    private ServerSocket ss;
    private List<String> streams;
    private String streamsDir;

    public Server(String []args, boolean debugMode, String bootstrapperIP){
        super(-1, debugMode, bootstrapperIP);
        this.streams = new ArrayList<>();
        this.streamsDir = args[1];
        
        if(this.streamsDir.charAt(this.streamsDir.length() - 1) != '/'){
            this.streamsDir += "/";
        }

        try{
            this.ss = new ServerSocket(Util.PORT);
        } catch(Exception e){
            e.printStackTrace();
        }
        
        this.logger.log(new LogEntry("Getting availabe Streams"));
        File path = new File(args[1]);
        File [] listOfFiles = path.listFiles();
        for(File f : listOfFiles){
            if(f.isFile()){
                this.streams.add(f.getName());
            }
        }
    }

    // Notify the RP about the available streams in this Server
    public boolean notifyStreamsRP(){
        int maxRetries = 10; // Maximum number of retries
        int retryInterval = 1000; // Delay between retries in milliseconds (1 second)
        int retryCount = 0;
        do {
            try{    
                // Send request
                ServerStreams sstreams = new ServerStreams(this.streams, this.id, this.ip);
                Socket s = new Socket(this.RPIPs.get(0), Util.PORT);
                TCPConnection tcpConnection = new TCPConnection(s);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                sstreams.serialize(out);
                out.flush();
                byte [] data = baos.toByteArray();
                tcpConnection.send(1, data); // Send the request to the RP
                this.logger.log(new LogEntry("Notifying Rendezvous Point about the available streams"));
                
                return true;
            }catch(ConnectException e) {
                this.logger.log(new LogEntry("ConnectException caught!!"));
                retryCount++;
                this.logger.log(new LogEntry("Unable to connect to RP. Retrying in " + retryInterval / 1000 + " seconds. Attempt " + retryCount));
                try {
                    Thread.sleep(retryInterval); // Wait before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread interrupted during retry wait.");
                    break;
                }
            }catch(Exception e){
                e.printStackTrace();
                break;
            }
        } while (retryCount < maxRetries);
        
        this.logger.log(new LogEntry("Server: Couldn't establish connection with RP."));

        return false;
    }   

    // Listens to new requests sent to the Server
    public void listen(){
        try{
            this.logger.log(new LogEntry("Now Listening to TCP requests"));
            while(true){
                Socket socket = this.ss.accept();
                TCPConnection tcpConnection = new TCPConnection(socket);
                Packet p = tcpConnection.receive();
                Thread t;

                // Creates different types of workers based on the type of packet received
                switch (p.type) {
                    // A thread is created that listens to the ServerStreams requests from the RP
                    case 1:
                        this.logger.log(new LogEntry("Received ServerStream request from the RP!"));
                        t = new Thread(new HandleServerStreams(this, tcpConnection));
                        t.start();
                        break;
                    case 2: // New video stream request
                        this.logger.log(new LogEntry("Received Video Stream Request from " + socket.getInetAddress().getHostAddress()));
                        t = new Thread(new HandleStreamRequests(tcpConnection, p, this.RPIPs.get(0), this));
                        t.start();
                        break;
                    case 5: // Flood message
                        this.logger.log(new LogEntry("Received flood message from " + socket.getInetAddress().getHostAddress()));
                        t = new Thread(new NormalFloodWorker(this, p));
                        t.start();
                        break;
                    case 7: // LIVENESS CHECK message
                        //this.log(new LogEntry("Received liveness check from " + socket.getInetAddress().getHostAddress()));
                        t = new Thread(new LivenessCheckWorker(this, tcpConnection, p));
                        t.start();
                        break;
                    default:
                        this.logger.log(new LogEntry("Packet type not recognized. Message ignored!"));
                        break;
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public void notifyEndOfStream(String streamName) {
        // Establishes TCP Connection with RP
        try {
            Socket socket = new Socket(this.RPIPs.get(0), Util.PORT);
            TCPConnection rpConnection = new TCPConnection(socket);

            NotificationEOS notificationEOS = new NotificationEOS(streamName);
            Packet packetEOS = new Packet(8, notificationEOS.serialize());
            rpConnection.send(packetEOS); // Send the end of stream notification to the RP
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getStreams(){
        return this.streams;
    }

    public void availableStreams(){
        System.out.println("Available Streams:");
        for(String stream : this.streams){
            System.out.println(stream);
        }
    }

    public String getStreamsDir(){
        return this.streamsDir;
    }

    public static void main(String args[]){
        List<String> argsL = new ArrayList<>();
        boolean debugMode = false;

        for (int i=0 ; i<args.length ; i++)
        {
            String arg = args[i];
            if (arg.equals("-g"))
                debugMode = true;
            else
                argsL.add(arg);
        }

        args = argsL.toArray(new String[0]);

        Server server = new Server(args, debugMode, args[0]);

        server.log(new LogEntry("Sending message to bootstrapper"));
        server.messageBootstrapper();

        // Tell the available streams to the RP 
        if (server.notifyStreamsRP()) {
            // notifyStreamsRP returns true if it went successful
            server.listen(); // Listen to new TCP requests
        }
    }
}

