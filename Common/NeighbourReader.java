package Common;

import java.util.Scanner;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class NeighbourReader 
{
    private String neighbourFile;
    private int id;
    private String rp;

    public NeighbourReader(int id, String neighbourFile)
    {
        this.id = id;
        this.neighbourFile = neighbourFile;
    }
    
    public String removeComment(String str)
    {
        int commentIdx = str.indexOf('#');
        if (commentIdx != -1)
            return str.substring(0, commentIdx);
        return str;
    }

    public Map<Integer, String> readNeighbours()
    {
        HashMap<Integer, String> neighbours = new HashMap<>();
        try
        {
            FileInputStream fis = new FileInputStream(neighbourFile);
            Scanner scanner = new Scanner(fis);

            // the RP is in the first line
            rp = removeComment(scanner.nextLine());

            int i=1;
            while (i++ < id)
                scanner.nextLine();
            
            String neighbourLine = removeComment(scanner.nextLine()); 
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

    public String getRPString ()
    {
        return rp;
    }
}