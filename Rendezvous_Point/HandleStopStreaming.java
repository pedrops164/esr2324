package Rendezvous_Point;

import Server.Server;

import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.StreamRequest;
import Common.LogEntry;
import Common.PathNode;
import Common.Path;
import Common.Util;
import Common.NotificationStopStream;
import Common.InvalidNodeException;

import java.net.*;
import java.util.*;

/*
 * - Responsible to handle stop streaming requests from the previous node (may be the client)
 * - This stop streaming request is propagated all the way to the RP
 * - This request is fault proof, so if the TCP connection can't be established with the next node in the path
 *   to the RP, it just skips that node and propagates to the next after than one.
 */
class HandleStopStreaming implements Runnable {
    private RP rp;
    private Packet stopStreamPacket;

    public HandleStopStreaming(RP rp, Packet stopStreamPacket) {
        this.rp = rp;
        this.stopStreamPacket = stopStreamPacket;
    }

    public void run() {
        this.rp.log(new LogEntry("Handling stop stream request!"));
        // get received bytes
        NotificationStopStream notificationStopStream = NotificationStopStream.deserialize(stopStreamPacket.data);
        // get stream name
        String streamName = notificationStopStream.getStreamName();
        // get path
        Path pathToRP = notificationStopStream.getPath();

        try {
            // Gets the previous node in the path
            PathNode previousNode = pathToRP.getPrevious(rp.getId());

            if (rp.isStreaming(streamName)) {
                // Stop streaming 'streamName' to the previous node
                rp.stopStreaming(streamName, previousNode.getNodeId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}