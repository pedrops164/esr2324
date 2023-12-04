package Overlay_Node;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.json.*;

public class BootstrapperHandler {
    private String configFile;
    private int RPID;
    private List<Integer> connected;
    private List<String> RPIPs;
    private Map<String, Integer> ipToId; // Maps node ip to node id
    private Map<Integer, List<String>> idToIps;
    private Map<Integer, Map<Integer, String>> idToNeighbours; // Maps node id to neighbours' ids
    private boolean changed;
    private ReentrantLock lock;
    private Condition verifyChanges, changes; 

    public BootstrapperHandler(String confingFile) {
        this.configFile = confingFile;
        this.ipToId = new HashMap<>();
        this.idToNeighbours = new HashMap<>();
        this.idToIps = new HashMap<>();
        this.connected = new ArrayList<>();
        loadConfig();
        this.lock = new ReentrantLock();
        this.verifyChanges = this.lock.newCondition();
        this.changes = this.lock.newCondition();
        this.changed = false;
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
        this.idToIps.put(id, new ArrayList<>());

        for (Object o : nodeObject.getJSONArray("ips"))
        {
            String ip = (String)o;
            this.ipToId.put(ip, id);
            this.idToIps.get(id).add(ip);
        }

        for (Object neighbour : nodeObject.getJSONArray("neighbours"))
        {
            JSONObject neighbourObj = ((JSONObject)neighbour);
            int neighbourId = neighbourObj.getInt("id");
            String neighbourIP = neighbourObj.getString("ip");

            this.idToNeighbours.get(id).put(neighbourId, neighbourIP);
        }
    }

    private boolean compareIPLists(List<String> l1, List<String> l2)
    {
        if (l1.size() != l2.size())
            return false;
        for (String s : l1)
            if (!l2.contains(s))
                return false;
        return true;
    }

    /**
     * Parse the config file and see what nodes changed
     * @return this list of node ids whose information changed
     */
    public List<Integer> parseChanges()
    {
        int oldRPID = this.RPID;
        List<String> oldRPIPs = this.RPIPs;
        Map<String, Integer> oldipToId = this.ipToId; // Maps node ip to node id
        Map<Integer, List<String>> oldidToIps = this.idToIps;
        Map<Integer, Map<Integer, String>> oldidToNeighbours = this.idToNeighbours; // Maps node id to neighbours' ids

        this.ipToId = new HashMap<>();
        this.idToNeighbours = new HashMap<>();
        this.idToIps = new HashMap<>();

        this.loadConfig();

        List<Integer> changedNodes = new ArrayList<>();

        if (oldRPID != this.RPID || !compareIPLists(oldRPIPs, this.RPIPs))
        {
            changedNodes.addAll(this.connected);
            return changedNodes;
        }

    }

    public void setChanged(boolean status)
    {
        this.lock.lock();
        try
        {
            this.changed = status;
            if (this.changed)
                this.verifyChanges.signalAll();
        }
        finally
        {
            this.lock.unlock();
        }
    }

    public void verifyChanged()
    {
        this.lock.lock();
        try
        {
            while (this.changed)
                this.changes.await();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            this.lock.unlock();
        }
    }

    public int getIdFromIP(String ip)
    {
        this.verifyChanged();
        if (!this.ipToId.containsKey(ip))
            return -1;
        return this.ipToId.get(ip);
    }

    public int getRPID()
    {
        this.verifyChanged();
        return this.RPID;
    }

    public List<String> getRPIPs()
    {
        this.verifyChanged();
        return this.RPIPs.stream().collect(Collectors.toList());
    }

    public List<String> getIPsfromID(int id)
    {
        this.verifyChanged();
        return this.idToIps.get(id).stream().collect(Collectors.toList());
    }

    public Map<Integer,String> getNeighboursFromID(int id)
    {
        this.verifyChanged();
        return new HashMap<>(this.idToNeighbours.get(id));
    }

    public void addConnected(int id)
    {
        this.connected.add(id);
    }
}