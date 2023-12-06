package Rendezvous_Point;

import Common.TCPConnection.Packet;
import Common.LogEntry;
import Common.PathNode;
import Common.Path;
import Common.NotificationStopStream;

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
        // get endStream
        boolean endStream = notificationStopStream.getEndStream();

        try {
            // Gets the previous node in the path
            PathNode previousNode = pathToRP.getPrevious(rp.getId());

            if (rp.isStreaming(streamName)) {
                // Stop streaming 'streamName' to the previous node
                rp.stopStreaming(streamName, previousNode.getNodeId());
            }
            
            // Notify the server to stop streaming
            if(endStream && this.rp.noNeighbours(streamName)) {
                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}