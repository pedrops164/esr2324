package Common;

import java.util.Comparator;

public class ComparePathDelay implements Comparator<Path> {

    @Override
    public int compare(Path firstPath, Path secondPath)
    {
        return Long.compare(firstPath.getTotalDelay(), secondPath.getTotalDelay());
    }
    
}
