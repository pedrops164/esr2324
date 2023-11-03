package Rendezvous_Point;

import Common.NeighbourReader;
import java.util.*;

public class RP{
    private int id;
    private Map<Integer, String> neighbours;
    
    // A ideia deste Map é ter para cada uma das streams existentes
    // a lista de IP's dos servidores que têm essas streams.
    private Map<Integer, List<String>> streamServers;
    private int streamCounter;

    public RP(String args[], NeighbourReader nr){
        this.id = Integer.parseInt(args[0]);
        this.neighbours = nr.readNeighbours();
    }

    
}