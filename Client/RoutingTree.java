package Client;

import Common.ComparePathDelay;
import Common.ComparePathJumps;
import Common.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RoutingTree {
    private List<Path> paths;
    public enum Heuristic { N_JUMPS, DELAY };
    private Heuristic heuristic;
    
    public RoutingTree()
    {
        this.paths = new ArrayList<>();
        this.heuristic = Heuristic.N_JUMPS;
    }    
    
    public RoutingTree(Heuristic heuristic)
    {
        this.paths = new ArrayList<>();
        this.heuristic = heuristic;
    }
    
    public Heuristic getHeuristic() {
        return heuristic;
    }
    
    public void setHeuristic(Heuristic heuristic) {
        this.heuristic = heuristic;
    }

    public void addPath(Path path)
    {
        this.paths.add(path);
    }

    public Path getBestPath()
    {
        Comparator<Path> c = (heuristic == Heuristic.DELAY) ?(new ComparePathDelay()) :(new ComparePathJumps());

        return paths.stream().min(c).get();
    }
}
