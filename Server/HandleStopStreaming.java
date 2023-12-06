package Server;

import Common.NotificationStopStream;
import Common.TCPConnection;

public class HandleStopStreaming implements Runnable{
    private Server server;
    private TCPConnection.Packet stopStreamingPacket;

    public HandleStopStreaming(Server server, TCPConnection.Packet p){
        this.server = server;
        this.stopStreamingPacket = p;
    }

    public void run(){
        // get received bytes
        NotificationStopStream notificationStopStream = NotificationStopStream.deserialize(stopStreamingPacket.data);
        
        // Make the server stop streaming this stream
        this.server.stopStreaming(notificationStopStream.getStreamName());
    }
}
