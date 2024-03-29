package Overlay_Node;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.json.*;

public class BootstrapperHandler {
    private String configFile;
    private int RPID;
    private Map<Integer,String> connected;
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
        this.connected = new HashMap<>();
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

    private boolean compareNeighbourMaps(Map<Integer,String> m1, Map<Integer,String> m2)
    {
        if (m1.size() != m2.size())
            return false;
        
        for (Map.Entry<Integer, String> entry : m1.entrySet())
            if (!m2.containsKey(entry.getKey()) || !m2.get(entry.getKey()).equals(entry.getValue()))
                return false;

        return true;
    }

    /**
     * Parse the config file and see what nodes changed
     * @return this list of node ids whose information changed
     */
    public Set<Integer> parseChanges(long waitTime, Set<String> removedNodes)
    {
        this.lock.lock();
        try
        { 
            boolean timePassed;
            do
            {
                timePassed = !this.verifyChanges.await(waitTime, TimeUnit.MILLISECONDS);
            }
            while(!this.changed && !timePassed); 

            int oldRPID = this.RPID;
            List<String> oldRPIPs = this.RPIPs;
            Map<String, Integer> oldipToId = this.ipToId; // Maps node ip to node id
            Map<Integer, List<String>> oldidToIps = this.idToIps;
            Map<Integer, Map<Integer, String>> oldidToNeighbours = this.idToNeighbours; // Maps node id to neighbours' ids
    
            this.ipToId = new HashMap<>();
            this.idToNeighbours = new HashMap<>();
            this.idToIps = new HashMap<>();
    
            this.loadConfig();
    
            Set<Integer> changedNodes = new HashSet<>(),
                        removedNodesIDs = new HashSet<>();
    
            if (oldRPID != this.RPID || !compareIPLists(oldRPIPs, this.RPIPs))
            {
                changedNodes.addAll(this.connected.keySet());
                return changedNodes;
            }
            
            for (Map.Entry<String,Integer> entry : oldipToId.entrySet())
            {
                String ip = entry.getKey();
                int oldID = entry.getValue();
                Integer newId = this.ipToId.get(ip); 
                if (newId!=null && oldID != newId.intValue())
                {
                    changedNodes.add(this.ipToId.get(ip));
                }
            }
    
            for (Map.Entry<Integer,List<String>> entry : oldidToIps.entrySet())
            {
                List<String> old = entry.getValue();
                if (!this.idToIps.containsKey(entry.getKey()))
                {
                    System.out.println("Nao ta no idToTps");
                    removedNodesIDs.add(entry.getKey());
                }
                else if (!compareIPLists(old, this.idToIps.get(entry.getKey())))
                {
                    changedNodes.add(entry.getKey());
                }
            }
    
            for (Map.Entry<Integer, Map<Integer, String>> entry : oldidToNeighbours.entrySet())
            {
                if (!this.idToNeighbours.containsKey(entry.getKey()))
                {
                    System.out.println("Nao ta no idToNeighbours");
                    removedNodesIDs.add(entry.getKey());
                }
                else if (!compareNeighbourMaps(entry.getValue(), this.idToNeighbours.get(entry.getKey())))
                    changedNodes.add(entry.getKey());
            }

            this.changed = false;
            this.changes.signalAll();
            
            
            removedNodesIDs.removeIf((id) -> !this.connected.containsKey(id));
            removedNodes.addAll(removedNodesIDs.stream().map((id) -> {
                return this.connected.get(id);
            }).toList());
            changedNodes.removeIf((id) -> (!this.connected.containsKey(id) || removedNodesIDs.contains(id)));

            return changedNodes;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            this.lock.unlock();
        }
        return new HashSet<>();
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

    public void addConnected(int id, String ip)
    {
        this.connected.put(id, ip);
    }

    public void removeConnected(int id)
    {
        this.connected.remove(id);
    }

    public void removeConnected(String ip)
    {
        int id = -1;
        for (Map.Entry<Integer, String> c : this.connected.entrySet())
        {
            if (ip.equals(c.getValue()))
                id = c.getKey();
        }
        if (id != -1)
            this.removeConnected(id);
    }
}