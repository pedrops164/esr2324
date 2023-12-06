package Rendezvous_Point;

import Common.LogEntry;
import Common.Node;
import Common.Path;
import Common.ServerStreams;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;

public class RP extends Node{
    private Map<String, List<Integer>> streamServers; // stream name to list of available servers
    private Map<Integer, String> servers; // serverID to serverIP
    private List<ServerRanking> rankedServers; // Organized list with the ranking from the best to the worst server 
    private Map<String, List<Integer>> streamNeighbours; // Maps each stream to the id's of the neighbours of want it 
    private Map<Integer, Path> paths; // maps clients to their respective paths (paths from RP to each client)
    private RPHandlerTCP rpHandlerTCP;
    private RPHandlerUDP rpHandlerUDP;
    private RPServerTester rpServerTester;

    public RP(String args[], boolean debugMode, String bootstrapperIP){
        super(-1, debugMode, bootstrapperIP);
        this.streamServers = new HashMap<>();
        this.servers = new HashMap<>();
        this.rankedServers = new ArrayList<>();
        this.paths = new HashMap<>();
        this.streamNeighbours = new HashMap<>();
    }

    public void run() {
        try {
            this.log(new LogEntry("Sending neighbour request to Bootstrapper"));
            int successfull = this.messageBootstrapper();

            if (successfull == 1)
            {
                System.out.println("Bootstrapper is not available.. Shutting down");
                return;
            }
            else if (successfull == 2)
            {
                System.out.println("This node is not on the overlay network.. Shutting down");
                return;
            }

            // Launch tcp worker
            this.rpHandlerTCP = new RPHandlerTCP(this);
            Thread tcp = new Thread(this.rpHandlerTCP);
            tcp.start();
            // Launch udp worker
            this.rpHandlerUDP = new RPHandlerUDP(this);
            Thread udp = new Thread(this.rpHandlerUDP);
            udp.start();
            this.rpServerTester = new RPServerTester(this);
            Thread serverTester = new Thread(this.rpServerTester);
            serverTester.start();

            // join threads
            tcp.join();
            udp.join();
            serverTester.join();
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

    public List<Integer> getNeighborIds(String streamName) {
        List<Integer> neighbours = this.streamNeighbours.get(streamName);
        return neighbours;
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

        // Remove possible copy of this server in the list
        this.rankedServers.removeIf(server -> server.getServerID() == sstreams.getID());
        for(ServerRanking sr: this.rankedServers){
            if(latency < sr.getLatency()){
                added = true;
                break;
            }
            i++;
        }

        if(added){
            // Add to a specific index
            this.rankedServers.add(i, new ServerRanking(sstreams.getID(), latency));
        }else{
            // Append to the list
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

    // Removes all the info of a server from the RP
    public void removeServer(int serverID){
        List<String> streamsToRemove = new ArrayList<>();
        this.servers.remove(serverID);
        this.rankedServers.removeIf(rs -> rs.getServerID() == serverID);
        for(Entry<String, List<Integer>> pair: this.streamServers.entrySet()){
            pair.getValue().removeIf(id -> id == serverID);
            if(pair.getValue().size() == 0){
                streamsToRemove.add(pair.getKey());
            }
        }
        
        for(String streamToRemove: streamsToRemove){
            this.streamServers.remove(streamToRemove);
        }
    }

    public void stopStreaming(String streamName) {
        this.streamNeighbours.remove(streamName);
    }
    
    // Stops this stream for the neighbour received as argument
    public void stopStreaming(String streamName, int nodeId) {
        List<Integer> neighbourIds = this.getNeighborIds(streamName);
        // removes the object, not the index!!
        if (neighbourIds!=null) {
            neighbourIds.remove(Integer.valueOf(nodeId));
        }
        // if the neighbourIds list is now empty for this stream, inform the server to stop streaming this stream!
    }

    public boolean isStreaming(String streamName) {
        return (this.streamNeighbours.containsKey(streamName) && !this.streamNeighbours.get(streamName).isEmpty());
    }

    public boolean noNeighbours(String streamName){
        return this.streamNeighbours.get(streamName).isEmpty();
    }

    public synchronized Map<Integer, String> getServers(){
        Map<Integer, String> clonedServers = new HashMap<>();
        clonedServers.putAll(this.servers);

        return clonedServers;
    }

    public void turnOff()
    {
        this.rpHandlerTCP.turnOff();
        this.rpHandlerUDP.turnOfF();
        this.rpServerTester.turnOff();
    }

    public static void main(String args[]){
        List<String> argsL = new ArrayList<>();
        boolean debugMode = false;

        for (int i=0 ; i<args.length ; i++)
        {
            String arg = args[i];
            if (arg.equals("-g"))
                debugMode = true;
            else
                argsL.add(arg);
        }

        args = argsL.toArray(new String[0]);
        RP rp = new RP(args, debugMode, args[0]);
        rp.run(); 
    }

}