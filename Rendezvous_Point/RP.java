package Rendezvous_Point;

import Common.LogEntry;
import Common.NeighbourReader;
import Common.Node;
import Common.Path;
import Common.ServerStreams;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RP extends Node{
    public static int RP_PORT = 333;
    
    private Map<String, List<Integer>> streamServers; // stream name to list of available servers
    private Map<Integer, String> servers; // serverID to serverIP
    private List<ServerRanking> rankedServers; // Organized list with the ranking from the best to the worst server 
    private Map<String, List<Integer>> streamNeighbours; // Maps each stream to the id's of the neighbours of want it 
    public Map<Integer, Path> paths; // maps clients to their respective paths (paths from RP to each client)

    public RP(String args[], NeighbourReader nr, boolean debugMode){
        super(Integer.parseInt(args[0]), nr, debugMode);
        this.streamServers = new HashMap<>();
        this.servers = new HashMap<>();
        this.rankedServers = new ArrayList<>();
        this.paths = new HashMap<>();
        this.streamNeighbours = new HashMap<>();
    }

    public void run() {
        try {
            // Launch tcp worker
            Thread tcp = new Thread(new RPHandlerTCP(this));
            tcp.start();
            // Launch udp worker
            Thread udp = new Thread(new RPHandlerUDP(this));
            udp.start();

            // join threads
            tcp.join();
            udp.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Associates client id to its path
    */
    public void addPathToClient(int clientId, Path path) {
        this.paths.put(clientId, path);
    }

    public void addStreamingFlux(String streamName, int neighbour){
        if(this.streamNeighbours.containsKey(streamName)){
            this.streamNeighbours.get(streamName).add(neighbour);
        }else{
            List<Integer> neighbours = new ArrayList<>();
            neighbours.add(neighbour);
            this.streamNeighbours.put(streamName, neighbours);
        }
    }

    public List<String> getNeighbourIpsStream(String streamName){
        List<Integer> neighbours = this.streamNeighbours.get(streamName);
        return this.getNeighboursIps(neighbours);
    }

    public synchronized void addServerStreams(int serverID, String serverIP, List<String> streams){

        this.logger.log(new LogEntry("Adding available streams from server " + serverIP));

        for (String stream : streams)
        {
            List<Integer> servers;
            if (this.streamServers.containsKey(stream))
                servers = this.streamServers.get(stream);
            else
                servers = new ArrayList<Integer>();
            servers.add(serverID);
            this.streamServers.put(stream, servers);
            this.servers.put(serverID, serverIP);
        }
    }

    public synchronized void rankServer(ServerStreams sstreams, LocalDateTime receivingTimeStamp){
        long latency = ChronoUnit.NANOS.between(sstreams.getTimeStamp(), receivingTimeStamp);
        int i = 0;
        boolean added = false;

        for(ServerRanking sr: this.rankedServers){
            if(latency < sr.getLatency()){
                added = true;
                break;
            }
            i++;
        }

        if(added){
            this.rankedServers.add(i, new ServerRanking(sstreams.getID(), latency));
        }else{
            this.rankedServers.add(new ServerRanking(sstreams.getID(), latency));
        }
    }

    // Get the best server that has a certain stream
    public synchronized String getServer(String streamName){
        List<Integer> servers = this.streamServers.get(streamName);
        for(ServerRanking sr: this.rankedServers){
            if(servers.contains(sr.getServerID())){
                return this.servers.get(sr.getServerID());  
            }
        }
        return "0.0.0.0";
    }

    public synchronized List<String> getAvailableStreams(){
        List<String> streams = new ArrayList<String>();
        for(Map.Entry<String, List<Integer>> entry : this.streamServers.entrySet()){
            streams.add(entry.getKey());
        }
        return streams;
    }

    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        boolean debugMode = Arrays.stream(args).anyMatch(s -> s.equals("-g"));
        RP rp = new RP(args, nr, debugMode);
        rp.run(); 
    }

}