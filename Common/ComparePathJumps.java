package Common;

import java.util.Comparator;

public class ComparePathJumps implements Comparator<Path> {
    
    @Override
    public int compare(Path firstPath, Path secondPath)
    {
        return Integer.compare(firstPath.getNoJumps(), secondPath.getNoJumps());
    }
}
