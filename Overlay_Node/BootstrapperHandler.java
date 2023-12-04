package Overlay_Node;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.json.*;

public class BootstrapperHandler {
    private String configFile;
    private int RPID;
    private List<String> RPIPs;
    private Map<String, Integer> ipToId; // Maps node ip to node id
    private Map<Integer, Map<Integer, String>> idToNeighbours; // Maps node id to neighbours' ids

    public BootstrapperHandler(String confingFile) {
        this.configFile = confingFile;
        this.ipToId = new HashMap<>();
        this.idToNeighbours = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        File config = new File(this.configFile);
        try 
        {
            String content = new String(Files.readAllBytes(Paths.get(config.toURI())));
            JSONObject mainObject = new JSONObject(content);
            
            JSONObject rpObject = mainObject.getJSONObject("RP");
            this.processRPObject(rpObject);

            for (Object o : mainObject.getJSONArray("nodes"))
            {
                this.processNode((JSONObject)o);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processRPObject(JSONObject rpObject)
    {
        this.RPID = rpObject.getInt("id");
        this.RPIPs = new ArrayList<>();
        for (Object o : rpObject.getJSONArray("ips"))
        {
            String ip = (String)o;
            this.RPIPs.add(ip);
        }
    }

    private void processNode(JSONObject nodeObject)
    {
        int id = nodeObject.getInt("id");
        this.idToNeighbours.put(id, new HashMap<>());

        for (Object o : nodeObject.getJSONArray("ips"))
        {
            String ip = (String)o;
            this.ipToId.put(ip, id);
        }

        for (Object neighbour : nodeObject.getJSONArray("neighbours"))
        {
            JSONObject neighbourObj = ((JSONObject)neighbour);
            int neighbourId = neighbourObj.getInt("id");
            String neighbourIP = neighbourObj.getString("ip");

            this.idToNeighbours.get(id).put(neighbourId, neighbourIP);
        }
    }

    public int getIdFromIP(String ip)
    {
        if (!this.ipToId.containsKey(ip))
            return -1;
        return this.ipToId.get(ip);
    }

    public int getRPID()
    {
        return this.RPID;
    }

    public List<String> getRPIPs()
    {
        return this.RPIPs.stream().collect(Collectors.toList());
    }

    public Map<Integer,String> getNeighboursFromID(int id)
    {
        return new HashMap<>(this.idToNeighbours.get(id));
    }
}