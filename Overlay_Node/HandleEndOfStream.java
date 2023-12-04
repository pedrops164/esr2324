package Overlay_Node;

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
    private ONode oNode;
    private Packet eosPacket;

    public HandleEndOfStream(ONode oNode, Packet eosPacket) {
        this.oNode = oNode;
        this.eosPacket = eosPacket;
    }

    public void run() {
        // get received bytes
        NotificationEOS notificationEOS = NotificationEOS.deserialize(eosPacket.data);
        // get stream name
        String streamName = notificationEOS.getStreamName();

        // If this stream is being streamed, stop streaming it, and propagate the EOS signal to the neighbours who want the stream
        if (oNode.isStreaming(streamName)) {
            List<String> neighbourIps = oNode.getNeighbourIpsStream(streamName);
            for (String neighbourIp: neighbourIps) {
                try {
                    this.oNode.log(new LogEntry("Sent EOS notification to " + neighbourIp));
                    Socket socket = new Socket(neighbourIp, Util.PORT);
                    TCPConnection neighbourConnection = new TCPConnection(socket);
                    neighbourConnection.send(this.eosPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            oNode.stopStreaming(streamName);
        }
    }

}