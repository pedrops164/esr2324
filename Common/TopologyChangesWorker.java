package Common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Common.TCPConnection.Packet;

public class TopologyChangesWorker implements Runnable {

    private Node node;
    private TCPConnection tcpConnection;
    public TopologyChangesWorker(Node node, TCPConnection tcpConnection)
    {
        this.node = node;
        this.tcpConnection = tcpConnection;
    }

    @Override
    public void run()
    {
        try 
        {
            this.tcpConnection.send(4, "OK".getBytes());
            Packet  pID = tcpConnection.receive(),
                        pIPs = tcpConnection.receive(),
                        pRPIPs = tcpConnection.receive(),
                        pNeighbours = tcpConnection.receive();
                
            this.node.setId(Util.deserializeInt(pID.data));

            List<?> auxIps = (List<?>)Util.deserializeObject(pIPs.data);
            this.node.setIp(auxIps.stream().map(s->(String)s).toList());
            
            List<?> aux = (List<?>)Util.deserializeObject(pRPIPs.data);
            this.node.RPIPs = aux.stream().map(s -> (String)s).toList();

            Map<?,?> auxMap = (Map<?,?>)Util.deserializeObject(pNeighbours.data);
            Map<Integer, String> neighbours = new HashMap<>();
            for (Map.Entry<?,?> entry : auxMap.entrySet())
            {
                neighbours.put((Integer)entry.getKey(), (String)entry.getValue());
            }
            this.node.setNeighbours(neighbours);

            tcpConnection.send(4, "OK".getBytes());
            this.node.log(new LogEntry("Received new id, RP information and neighbours from Bootstrapper"));
            tcpConnection.stopConnection();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }
    
}
