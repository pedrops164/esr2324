import Common.NeighbourReader;

public class RP {
    private int id;
    private Map<Integer, String> neighbours;
    
    // A ideia deste Map é ter para cada uma das streams existentes
    // a lista de IP's dos servidores que têm essas streams.
    private Map<Integer, List<String>> streamServers;
    private int streamCounter;

    public static void main(String args[]){
        this.id = Integer.parseInt(args[1]);
        NeighbourReader nr = new NeighbourReader(this.id, args[0]);
        this.neighbours = nr.readNeighbours();

        // Testar a leitura dos neighbours
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }
}