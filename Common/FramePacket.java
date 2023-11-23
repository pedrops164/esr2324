package Common;

import Common.Path;
import Common.UDPDatagram;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class FramePacket {
    private Path path;
    private UDPDatagram udpDatagram;

    public FramePacket(Path path, UDPDatagram packet) {
        this.path = path;
        this.udpDatagram = packet;
    }

    public byte[] serialize() {
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            // Serialize the Path
            byte[] pathBytes = path.serialize();
            out.writeInt(pathBytes.length);
            out.write(pathBytes);

            // Serialize the UDPDatagram
            this.udpDatagram.serialize(out);
            out.flush();
            byte[] fpBytes = baos.toByteArray();
            return baos.toByteArray();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static FramePacket deserialize(byte[] receivedBytes) {
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
            DataInputStream in = new DataInputStream(bais);

            // Deserialize the Path
            int pathLength = in.readInt();
            byte[] pathBytes = new byte[pathLength];
            in.readFully(pathBytes);
            Path path = Path.deserialize(pathBytes);

            // Deserialize the UDPDatagram
            UDPDatagram datagram = UDPDatagram.deserialize(in);

            // return the de-serialized FramePacket
            return new FramePacket(path, datagram);
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