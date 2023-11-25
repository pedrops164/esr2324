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

public class FramePacket implements Serializable {
    private Path path;
    private UDPDatagram udpDatagram;

    public FramePacket(Path path, UDPDatagram packet) {
        this.path = path;
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

    public Path getPath() {
        return this.path;
    }
    
    public UDPDatagram getUDPDatagram() {
        return this.udpDatagram;
    }
}