package Common;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;

public class PathNode {
    private int nodeId, nodePort;
    private IPAddress nodeIPAddress;    
    private LocalDateTime timeStamp;
    
    public PathNode (int nodeID, int nodePort, String nodeIPAddress)
    {
        this.nodeId = nodeID;
        this.nodePort = nodePort;
        this.nodeIPAddress = new IPAddress(nodeIPAddress);
        this.timeStamp = LocalDateTime.now();
    }

    public PathNode (byte[] arr)
    {
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        this.nodeId = buffer.getInt(0*4);
        this.nodePort = buffer.getInt(1*4);
        this.nodeIPAddress = IPAddress.deserialize(Arrays.copyOfRange(arr, 2*4, 6*4));
        int year = buffer.getInt(6*4);
        int month = buffer.getInt(7*4);
        int day = buffer.getInt(8*4);
        int hour = buffer.getInt(9*4);
        int minute = buffer.getInt(10*4);
        int second = buffer.getInt(11*4);
        int nano = buffer.getInt(12*4);
        this.timeStamp = LocalDateTime.of(year, month, day, hour, minute, second, nano);
    }
    
    public int getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }
    
    public IPAddress getNodeIPAddress() {
        return nodeIPAddress;
    }
    
    public void setNodeIPAddress(IPAddress nodeIPAddress) {
        this.nodeIPAddress = nodeIPAddress;
    }
    
    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }
    
    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o instanceof Integer)
            return (int)o == nodeId;

        if (o == null || o.getClass() != this.getClass())
            return false;
        
        PathNode node = (PathNode)o;
        return  node.getNodeId() == nodeId && 
                node.getNodePort() == nodePort &&
                node.getNodeIPAddress().equals(nodeIPAddress);
    }

    @Override
    public String toString()
    {
        return "PathNode{ nodeId: " + nodeId + 
                        " nodePort: " + nodePort + 
                        " nodeIPAddress: " + nodeIPAddress + " }";
    }

    public byte[] serialize()
    {
        ByteBuffer bb = ByteBuffer.allocate(13*4);
        bb = bb.putInt(getNodeId());
        bb = bb.putInt(getNodePort());
        bb = bb.put(this.nodeIPAddress.serialize());
        bb = bb.putInt(timeStamp.getYear());
        bb = bb.putInt(timeStamp.getMonthValue());
        bb = bb.putInt(timeStamp.getDayOfMonth());
        bb = bb.putInt(timeStamp.getHour());
        bb = bb.putInt(timeStamp.getMinute());
        bb = bb.putInt(timeStamp.getSecond());
        bb = bb.putInt(timeStamp.getNano());

        return bb.array();
    }

    public static PathNode deserialize(byte[] arr)
    {
        return new PathNode(arr);
    }
}
