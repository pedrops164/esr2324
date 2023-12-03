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
    private int popQuantity;

    public RPDatagramPacketQueue(int popQuantity) 
    {
        this.packets = new ArrayList<>();
        this.packetsLock = new ReentrantLock();
        this.packetsAvailable = this.packetsLock.newCondition();
        this.popQuantity = popQuantity;
    }

    public void pushPacket(DatagramPacket datagramPacket) 
    {
        this.packetsLock.lock();

        try {
            this.packets.add(datagramPacket);
            if (this.packets.size() >= this.popQuantity) {
                this.packetsAvailable.signal();
            }
        } finally {
            this.packetsLock.unlock();
        }

    }

    public List<DatagramPacket> popPackets() {
        ArrayList<DatagramPacket> returnPackets = null;
        this.packetsLock.lock();

        try {
            while (this.packets.size() < this.popQuantity) {
                this.packetsAvailable.await();
            }

            returnPackets = new ArrayList<>(this.packets.subList(0, this.popQuantity));
            this.packets.removeAll(returnPackets);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.packetsLock.unlock();
        }

        return returnPackets;
    }
}