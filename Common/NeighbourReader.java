package Common;

import java.util.Scanner;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class NeighbourReader 
{
    private String neighbourFile;
    private int id;

    public NeighbourReader(int id, String neighbourFile)
    {
        this.id = id;
        this.neighbourFile = neighbourFile;
    }
    
    public Map<Integer, String> readNeighbours()
    {
        HashMap<Integer, String> neighbours = new HashMap<>();
        try
        {
            FileInputStream fis = new FileInputStream(neighbourFile);
            Scanner scanner = new Scanner(fis);

            int i=0;
            while (i++ < id)
                scanner.nextLine();
            
            String neighbourLine = scanner.nextLine(); 
            String[] pairs = neighbourLine.split(" ");

            for (String pair : pairs)
            {
                String[] neighbour = pair.split("[,:]");
                if (neighbour.length != 2)
                {
                    scanner.close();
                    throw new InvalidFormatException("File is invalid in line " + (id+1));
                }
                
                int idN = Integer.parseInt(neighbour[0]);

                neighbours.put(idN, neighbour[1]);
            }
        
            scanner.close();
        } catch (Exception e) 
        {
            e.printStackTrace();
        }

        return neighbours;
    }    
}