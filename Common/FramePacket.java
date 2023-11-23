package Common;

import Common.Path;
import Common.UDPDatagram;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class FramePacket {
    private Path path;
    private UDPDatagram udpDatagram;

    public FramePacket(Path path, UDPDatagram packet) {
        this.path = path;
        this.udpDatagram = packet;
    }

    public void serialize(DataOutputStream out) {
        try{
            // Serialize the Path
            byte[] pathBytes = path.serialize();
            out.writeInt(pathBytes.length);
            out.write(pathBytes);

            // Serialize the UDPDatagram
            this.udpDatagram.serialize(out);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static FramePacket deserialize(DataInputStream in) {
        try{
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
}