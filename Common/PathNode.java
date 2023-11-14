package Common;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;

public class PathNode {
    private int nodeId, nodePort;
    private String nodeIpAdress;    
    private LocalDateTime timeStamp;
    
    public PathNode (int nodeID, int nodePort, String nodeIpAdress)
    {
        this.nodeId = nodeID;
        this.nodePort = nodePort;
        this.nodeIpAdress = nodeIpAdress;
        this.timeStamp = LocalDateTime.now();
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
    
    public String getNodeIpAdress() {
        return nodeIpAdress;
    }
    
    public void setNodeIpAdress(String nodeIpAdress) {
        this.nodeIpAdress = nodeIpAdress;
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
                node.getNodeIpAdress().equals(nodeIpAdress);
    }

    @Override
    public String toString()
    {
        return "PathNode{ nodeId: " + nodeId + 
                        " nodePort: " + nodePort + 
                        " nodeIpAddress: " + nodeIpAdress + "}";
    }

    public static byte[] serialize(PathNode node)
    {
        int strLens = node.getNodeIpAdress().length() + node.getTimeStamp().toString().length();
        ByteBuffer bb = ByteBuffer.allocate(4 + 4 + strLens);
        bb = bb.putInt(node.getNodeId());
        bb = bb.putInt(node.getNodePort());
        bb = bb.put(node.getNodeIpAdress().getBytes());
        bb = bb.put(node.getTimeStamp().toString().getBytes());

        return bb.array();
    }
}
