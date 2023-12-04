package Client;

import Server.Server;

import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.StreamRequest;
import Common.LogEntry;
import Common.PathNode;
import Common.Util;
import Common.NotificationEOS;

import java.net.*;
import java.util.*;

// Responsible to handle End of stream notifications from the server
// This eos notification is propagated all the way to the clients receiving the stream
class HandleEndOfStream implements Runnable {
    private Client client;
    private Packet eosPacket;

    public HandleEndOfStream(Client client, Packet eosPacket) {
        this.client = client;
        this.eosPacket = eosPacket;
    }

    public void run() {
        // get received bytes
        NotificationEOS notificationEOS = NotificationEOS.deserialize(eosPacket.data);
        // get stream name
        String streamName = notificationEOS.getStreamName();

        // If this stream is being streamed, stop streaming it
        this.client.log(new LogEntry("Received End of Stream Notification!"));
        client.cvm.streamEnded(streamName);
    }

}