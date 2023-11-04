package Server;

import Common.NeighbourReader;

public class Main {
    public static void main(String args[]){
        NeighbourReader nr = new NeighbourReader(Integer.parseInt(args[0]), args[1]);
        Server server = new Server(args, nr);

        // Tell the available streams to the RP
        server.notifyStreamsRP();
    }
}