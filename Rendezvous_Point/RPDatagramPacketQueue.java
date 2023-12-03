// Source code is decompiled from a .class file using FernFlower decompiler.
package Rendezvous_Point;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RPDatagramPacketQueue {
    private List<DatagramPacket> packets;
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
            this.packets.addAll(datagramPackets);
            this.packetsAvailable.signal();
        } finally {
            this.packetsLock.unlock();
        }

    }

    public DatagramPacket popPackets() {
        DatagramPacket returnPacket = null;
        this.packetsLock.lock();

        try {
            while (this.packets.size() < 1) {
                this.packetsAvailable.await();
            }

            returnPacket = this.packets.remove(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.packetsLock.unlock();
        }

        return returnPacket;
    }
}