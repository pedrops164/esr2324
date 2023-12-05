package Overlay_Node;

import Common.LogEntry;
import Common.Node;
import java.util.*;

public class ONode extends Node {
    private Map<String, List<Integer>> streamNeighours;
    private BootstrapperHandler bootstrapperHandler;
    private String bootstrapConfigFile;
    private ONodeHandlerTCP oNodeHandlerTCP;
    private ONodeHandlerUDP oNodeHandlerUDP;


    public ONode(String bootstrapperIP, boolean debugMode)
    {
        super(-1, debugMode, bootstrapperIP);
        this.streamNeighours = new HashMap<>();
        this.bootstrapperHandler = null;
    }

    public ONode(int id, String bootstrapConfigFile, boolean debugMode)
    {
        super(id, debugMode, "localhost");
        this.streamNeighours = new HashMap<>();
        this.bootstrapConfigFile = bootstrapConfigFile;
        this.bootstrapperHandler = new BootstrapperHandler(this.bootstrapConfigFile);
        this.neighbours = this.bootstrapperHandler.getNeighboursFromID(id);
        this.RPIPs = this.bootstrapperHandler.getRPIPs();
        this.ips = this.bootstrapperHandler.getIPsfromID(this.id);
    }

    public int getId()
    {
        return id;
    } 

    public Map<Integer, String> getNeighbours()
    {
        return neighbours;
    }

    public boolean alreadyStreaming(String stream){
        if(this.streamNeighours.containsKey(stream)) 
            return true;
        
        return false;
    }

    public void addStreamingFlux(String streamName, int neighbour){

        if(this.streamNeighours.containsKey(streamName)){
            this.streamNeighours.get(streamName).add(neighbour);
        }else{
            List<Integer> neighbours = new ArrayList<>();
            neighbours.add(neighbour);
            this.streamNeighours.put(streamName, neighbours);
        }
    }

    public List<String> getNeighbourIpsStream(String streamName){
        List<Integer> neighbours = this.getNeighborIds(streamName);
        return this.getNeighboursIps(neighbours);
    }

    public List<Integer> getNeighborIds(String streamName) {
        List<Integer> neighbours = this.streamNeighours.get(streamName);
        return neighbours;
    }


    public void run()
    {
        try {
            Thread changesWorker = null;
            if (!this.isBoostrapper())
            {
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
            }
            else
            {
                changesWorker = new Thread(new BoostrapperChangesWorker(this, this.bootstrapperHandler));
                changesWorker.start();
            }

            // Launch tcp worker
            this.oNodeHandlerTCP = new ONodeHandlerTCP(this);
            Thread tcp = new Thread(this.oNodeHandlerTCP);
            tcp.start();
            // Launch udp worker
            this.oNodeHandlerUDP = new ONodeHandlerUDP(this);
            Thread udp = new Thread(this.oNodeHandlerUDP);
            udp.start();

            // join threads
            tcp.join();
            udp.join();
            if (this.isBoostrapper())
                changesWorker.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Stops this stream for all clients
    public void stopStreaming(String streamName) {
        this.streamNeighours.remove(streamName);
    }

    // Stops this stream for the client received as argument
    public void stopStreaming(String streamName, int nodeId) {
        List<Integer> neighbourIds = this.getNeighborIds(streamName);
        // removes the object, not the index!!
        if (neighbourIds!=null) {
            neighbourIds.remove(Integer.valueOf(nodeId));
        }
    }

    public boolean isStreaming(String streamName) {
        return this.streamNeighours.containsKey(streamName);
    }

    public BootstrapperHandler getBootstrapperHandler()
    {
        return this.bootstrapperHandler;
    }

    public boolean isBoostrapper()
    {
        return this.bootstrapperHandler != null;
    }

    public void turnOff()
    {
        this.oNodeHandlerTCP.turnOff();
        this.oNodeHandlerUDP.turnOff();
    }

    public static void main(String args[]){
        List<String> argsL = new ArrayList<>();
        boolean debugMode = false;
        boolean bootstrapMode = !args[0].matches("[0-9]+.[0-9]+.[0-9]+.[0-9]+");

        for (int i=0 ; i<args.length ; i++)
        {
            String arg = args[i];
            if (arg.equals("-g"))
                debugMode = true;
            else
                argsL.add(arg);
        }

        args = argsL.toArray(new String[0]);

        ONode onode;
        if (bootstrapMode)
        {
            onode = new ONode(Integer.parseInt(args[0]), args[1], debugMode);
        }
        else
        {
            onode = new ONode(args[0], debugMode);
        }
        onode.run();
    }
}

