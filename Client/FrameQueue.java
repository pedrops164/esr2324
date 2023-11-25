package Client;

import Common.UDPDatagram;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

class FrameQueue {
    private PriorityQueue<UDPDatagram> queue;
    private int currentTimeStamp;

    public FrameQueue() {
        this.queue = new PriorityQueue<>();
        this.currentTimeStamp = 0;
    }

    public synchronized void addPacket(UDPDatagram packet) {
        if (packet.getTimeStamp() > this.currentTimeStamp) {
            queue.add(packet);
        }
    }

    public synchronized UDPDatagram getNextFrame() throws NoNextFrameException{
        if (!queue.isEmpty()) {
            UDPDatagram ret = queue.poll();
            this.currentTimeStamp = ret.getTimeStamp();
            return ret;
        }
        throw new NoNextFrameException();
    }

    public synchronized void updateCurrentTimeStamp(int timeStamp) {
        currentTimeStamp = timeStamp;
        // Remove outdated packets
        while (!queue.isEmpty() && queue.peek().getTimeStamp() < currentTimeStamp) {
            queue.poll();
        }
    }
}

class NoNextFrameException extends Exception {
    public NoNextFrameException()
    {
        super("Frame Queue is Empty!");
    }
 }