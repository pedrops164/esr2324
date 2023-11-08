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

    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        Client c = new Client(args, nr);
    }
}   
