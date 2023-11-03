package Rendezvous_Point;

import Common.NeighbourReader;

public class Main {
    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        RP rp = new RP(args, nr);
    }
}