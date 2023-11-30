package Rendezvous_Point;

public class ServerRanking {
    private int serverID;
    private long lantecy;

    public ServerRanking(int sid, long lantecy){
        this.serverID = sid;
        this.lantecy = lantecy;
    }

    public int getServerID(){
        return this.serverID;
    }
    
    public long getLatency(){
        return this.lantecy;
    }

    public String toString(){
        return "ServerID: " + this.serverID + "\nLatency: " + this.lantecy;
    }
}
