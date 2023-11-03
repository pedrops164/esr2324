package Client;

import Common.NeighbourReader;
import java.util.*;

public class Client {
    private int id;
    private Map<Integer, String> neighbours;

    public Client(String args[], NeighbourReader nr){
        this.id = Integer.parseInt(args[0]);
        this.neighbours = nr.readNeighbours(); 
    }
}   
