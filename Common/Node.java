package Common;

import java.util.Map;

public abstract class Node {
    protected int id;
    protected String ip;
    protected String RPIP;
    protected Map<Integer, String> neighbours;
    
    public Node(int id, NeighbourReader nr)
    {
        this.id = id;
        this.neighbours = nr.readNeighbours();
        this.RPIP = nr.getRPString();
        this.ip = this.neighbours.get(id);
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public Map<Integer, String> getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(Map<Integer, String> neighbours) {
        this.neighbours = neighbours;
    }
}
