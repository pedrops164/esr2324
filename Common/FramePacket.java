package Common;

import Common.Path;
import Common.UDPDatagram;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectInput;
import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

public class FramePacket implements Serializable {
    private List<Path> paths;
    private UDPDatagram udpDatagram;

    public FramePacket(List<Path> paths, UDPDatagram packet) {
        this.paths = paths;
        this.udpDatagram = packet;
    }

    public byte[] serialize() {
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(baos);
            out.writeObject(this);
            byte b[] = baos.toByteArray();
            out.close();
            baos.close();
            return b;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static FramePacket deserialize(byte[] receivedBytes) {
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
            ObjectInput in = new ObjectInputStream(bais);

            FramePacket ret = (FramePacket) in.readObject();
            return ret;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public List<Path> getPaths() {
        return this.paths;
    }
    
    public UDPDatagram getUDPDatagram() {
        return this.udpDatagram;
    }
}