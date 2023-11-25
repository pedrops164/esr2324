package Common;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.io.Serializable;

public class Path implements Serializable {
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
    
    public Path(byte[] arr) 
    {
        this.nodeList = new ArrayList<>();
        for (int i=0 ; i<arr.length ; i+=13*4)
        {
            nodeList.add(PathNode.deserialize(Arrays.copyOfRange(arr, i, i+13*4)));
        }
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

    public int getNoJumps()
    {
        return nodeList.size() - 1;
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

    public PathNode getClient()
    {
        return this.nodeList.get(0);
    }

    public PathNode getLast()
    {
        return this.nodeList.get(this.nodeList.size()-1);
    }

    public boolean inPath(PathNode pathNode)
    {
        return this.nodeList.contains(pathNode);
    }

    public boolean inPath(int id)
    {
        return this.nodeList.stream().anyMatch(pn -> (pn.getNodeId() == id));
    }

    @Override
    public String toString()
    {
        String r = "Path{\n";
        for (PathNode pn : nodeList)
            r += " " + pn + " \n";
        return r + "}";
    }
    
    public byte[] serialize()
    {
        ByteBuffer buffer = ByteBuffer.allocate(nodeList.size() * 13 * 4);

        for (PathNode node : nodeList)
        {
            byte[] arr = node.serialize();
            buffer.put(arr);
        }

        return buffer.array();
    }

    public static Path deserialize (byte[] arr)
    {
        return new Path(arr);
    }

    // Main used to test serialize and deserialize
    // public static void main(String[] args) {
    //     Path p = new Path();
    //     int[] arr = {192, 168, 56, 100};

    //     for (int i=0 ; i<10 ; arr[3]++, i++)
    //     {
    //         p.addNode(new PathNode(i+1, 333, new IPAddress(arr).toString()));
    //     }

    //     System.out.println(deserialize(p.serialize()));
    // }

    public int nextNode(int currentNodeId) {
        if (this.nodeList == null || this.nodeList.isEmpty()) {
            System.out.println("Path is empty");
            return -1;
        }

        PathNode previousNode = null;
        for (PathNode node : this.nodeList) {
            if (node.getNodeId() == currentNodeId) {
                // If currentNodeId is the first node, there is no previous node
                return previousNode == null ? -1 : previousNode.getNodeId();
            }
            previousNode = node;
        }

        // currentNodeId not found in the list
        return -1;
    }
}
