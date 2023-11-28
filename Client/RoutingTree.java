package Client;

import Common.ComparePathDelay;
import Common.ComparePathJumps;
import Common.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public void removePath(Path path)
    {
        int idx = this.paths.indexOf(path);
        if (idx != -1)
            this.paths.remove(idx);
    }

    public Path getBestPath() throws NoPathsAvailableException
    {
        if (paths.size() == 0)
            throw new NoPathsAvailableException("Exception: There are no available paths yet");
        Comparator<Path> c = (heuristic == Heuristic.DELAY) ?(new ComparePathDelay()) :(new ComparePathJumps());

        return paths.stream().min(c).get();
    }

    public List<Path> getOrderedPaths()
    {
        Comparator<Path> c = (heuristic == Heuristic.DELAY) ?(new ComparePathDelay()) :(new ComparePathJumps());
        return this.paths.stream().sorted(c).collect(Collectors.toList());
    }

    public boolean isAvailable()
    {
        return this.paths.size() > 0;
    }
}
