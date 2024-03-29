// Source code is decompiled from a .class file using FernFlower decompiler.
package Rendezvous_Point;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class RPDatagramPacketQueue {
    private List<List<DatagramPacket>> packets;
    private ReentrantLock packetsLock;
    private Condition packetsAvailable;

    public RPDatagramPacketQueue() 
    {
        this.packets = new ArrayList<>();
        this.packetsLock = new ReentrantLock();
        this.packetsAvailable = this.packetsLock.newCondition();
    }

    public void pushPackets(List<DatagramPacket> datagramPackets) 
    {
        this.packetsLock.lock();

        try {
            this.packets.add(datagramPackets.stream().collect(Collectors.toList()));
            this.packetsAvailable.signal();
        } finally {
            this.packetsLock.unlock();
        }

    }

    public List<DatagramPacket> popPackets() {
        List<DatagramPacket> returnPackets = null;
        this.packetsLock.lock();

        try {
            while (this.packets.size() < 1) {
                this.packetsAvailable.await();
            }

            returnPackets = this.packets.remove(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.packetsLock.unlock();
        }

        return returnPackets;
    }
}