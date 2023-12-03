package Bootstrapper;

import java.util.*;


public class Bootstrapper {
    public static String configFile = "config_file.txt";
    private Map<String, Integer> ipToId; // Maps node ip to node id
    private Map<Integer, List<Integer>> idToNeighbours; // Maps node id to neighbours' ids

    public Bootstrapper() {
        this.ipToId = new HashMap<>();
        this.idToNeighbours = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    processFirstLine(line);
                    isFirstLine = false;
                } else {
                    processOtherLines(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processFirstLine(String line) {
        // Example: "7:10.0.19.1 # RP"
        String[] parts = line.split(" ")[0].split(":"); // Split by space and then by colon
        int id = Integer.parseInt(parts[0]);
        String ip = parts[1];
        ipToId.put(ip, id);
    }

    private void processOtherLines(String line) {
        // Example: "10.0.18.10 2:10.0.16.1"
        String[] parts = line.split(" ");
        String nodeIp = parts[0];
        int nodeId = -1;

        List<Integer> neighbours = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String[] neighbourParts = parts[i].split(":");
            int neighbourId = Integer.parseInt(neighbourParts[0]);
            String neighbourIp = neighbourParts[1];

            if (nodeId == -1) {
                nodeId = ipToId.computeIfAbsent(nodeIp, k -> neighbourId);
            }

            neighbours.add(neighbourId);
            ipToId.putIfAbsent(neighbourIp, neighbourId);
        }

        idToNeighbours.put(nodeId, neighbours);
    }

    // Main method for testing
    public static void main(String[] args) {
        Bootstrapper bootstrapper = new Bootstrapper();
        System.out.println("IP to ID: " + bootstrapper.getIpToId());
        System.out.println("ID to Neighbours: " + bootstrapper.getIdToNeighbours());
    }
}