package Rendezvous_Point;

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
    private RP rp;
    private Packet eosPacket;

    public HandleEndOfStream(RP rp, Packet eosPacket) {
        this.rp = rp;
        this.eosPacket = eosPacket;
    }

    public void run() {
        // get received bytes
        NotificationEOS notificationEOS = NotificationEOS.deserialize(eosPacket.data);
        // get stream name
        String streamName = notificationEOS.getStreamName();

        // If this stream is being streamed, stop streaming it, and propagate the EOS signal to the neighbours who want the stream
        if (rp.isStreaming(streamName)) {
            List<String> neighbourIps = rp.getNeighbourIpsStream(streamName);
            for (String neighbourIp: neighbourIps) {
                try {
                    this.rp.log(new LogEntry("Sent EOS notification to " + neighbourIp));
                    Socket socket = new Socket(neighbourIp, Util.PORT);
                    TCPConnection neighbourConnection = new TCPConnection(socket);
                    neighbourConnection.send(this.eosPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            rp.stopStreaming(streamName);
        }
    }

}