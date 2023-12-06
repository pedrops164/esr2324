package Overlay_Node;

import Common.TCPConnection;
import Common.TCPConnection.Packet;
import Common.LogEntry;
import Common.PathNode;
import Common.Path;
import Common.Util;
import Common.NotificationStopStream;
import Common.InvalidNodeException;

import java.net.*;

// Responsible to handle stop streaming requests from the previous node (may be the client)
// This stop streaming request is propagated all the way to the RP
// This request is fault proof, so if the TCP connection can't be established with the next node in the path
// to the RP, it just skips that node and propagates to the next after than one.
class HandleStopStreaming implements Runnable {
    private ONode oNode;
    private Packet stopStreamPacket;

    public HandleStopStreaming(ONode oNode, Packet stopStreamPacket) {
        this.oNode = oNode;
        this.stopStreamPacket = stopStreamPacket;
    }

    public void run() {
        this.oNode.log(new LogEntry("Handling stop stream request!"));
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
            PathNode previousNode = pathToRP.getPrevious(oNode.getId());
            // Gets the next node in the path
            PathNode nextNode = pathToRP.getNext(oNode.getId());

            if (oNode.isStreaming(streamName)) {
                // Stop streaming 'streamName' to the previous node
                this.oNode.log(new LogEntry("previous node id - " + previousNode.getNodeId()));
                this.oNode.log(new LogEntry("Stopped streaming - " + streamName));
                oNode.stopStreaming(streamName, previousNode.getNodeId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We only keep spreading the StopStreaming if there are no more neighbours wanting this stream of if we don't want to end the stream
        if(this.oNode.noNeighbours(streamName) || endStream == false){
            // Propagate the Stop streaming request to the next node
            // If the TCP connection fails, try to connect with the next node after that, and so on
            PathNode currentNode = pathToRP.getCurrent(oNode.getId());
            while (true) {
                try {
                    // get next node
                    currentNode = pathToRP.getNext(currentNode.getNodeId());
                    // Try to establish TCP connection with the next node
                    Socket socket = new Socket(currentNode.getNodeIpAddressStr(), Util.PORT);
                    TCPConnection neighbourConnection = new TCPConnection(socket);
                    // Propagate the stop stream request to the neighbor
                    neighbourConnection.send(this.stopStreamPacket);
                    break;
                } catch (InvalidNodeException e) {
                    this.oNode.log(new LogEntry("Iterated through all nodes"));
                    break;
                } catch (Exception e) {
                    // if we couldn't establish tcp connection with the next, continue to the next iteration
                    //continue;
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}