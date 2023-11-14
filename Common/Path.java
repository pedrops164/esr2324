package Common;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Path {
    private List<PathNode> nodeList;
    
    
    public Path()
    {
        this.nodeList = new ArrayList<>();    
    }
    
    public Path(PathNode firstNode)
    {
        this.nodeList = new ArrayList<>();    
        this.addNode(firstNode);    
    }
    
    public void addNode(PathNode pathNode)
    {
        this.nodeList.add(pathNode);
    }
    
    public long getTotalDelay()
    {
        long time = 0;
        LocalDateTime last = nodeList.get(0).getTimeStamp();
        for (int i=1 ; i<this.nodeList.size() ; i++)
        {
            LocalDateTime current = nodeList.get(i).getTimeStamp();
            time += ChronoUnit.MILLIS.between(last, current);
            last = current;
        }
        
        return time;
    }
    
    public List<PathNode> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<PathNode> nodeList) {
        this.nodeList = nodeList;
    }

    public int indexOf(int id)
    {
        return nodeList.stream().map(PathNode::getNodeId).collect(Collectors.toList()).indexOf(id);
    }

    public PathNode getNext(int id) throws InvalidNodeException
    {
        int index = this.indexOf(id);
        if (index == -1)
            throw new InvalidNodeException("Node with id '" + id + "' not in path...");
        if (index == nodeList.size()-1)
            throw new InvalidNodeException("Node with id '" + id + "' is at the end of the path..");
        
        return nodeList.get(index+1);
    }

    @Override
    public String toString()
    {
        String r = "Path{";
        for (PathNode pn : nodeList)
            r += " " + pn + " ";
        return r + "}";
    }
    
    public static byte[] serialize(Path path)
    {
        int totalSize = 0;
        List<byte[]> bytes = new ArrayList<>();

        for (PathNode node : path.getNodeList())
        {
            byte[] arr = PathNode.serialize(node);
            bytes.add(arr);
            totalSize += arr.length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        for (byte[] arr : bytes)
            buffer.put(arr);

        return buffer.array();
    }

}
