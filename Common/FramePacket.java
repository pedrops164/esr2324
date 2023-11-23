package Common;

import Common.Path;
import Common.RTPpacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class FramePacket {
    private Path path;
    private RTPpacket packet;

    public FramePacket(Path path, RTPpacket packet) {
        this.path = path;
        this.packet = packet;
    }

    public void serialize(DataOutputStream out) {
        try{
            // Serialize the Path
            byte[] pathBytes = path.serialize();
            out.writeInt(pathBytes.length);
            out.write(pathBytes);

            // Serialize the RTPpacket
            byte[] packetBytes = new byte[this.packet.getlength()];
            int packetLength = this.packet.getpacket(packetBytes);
            out.writeInt(packetLength);
            out.write(packetBytes);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static FramePacket deserialize(DataInputStream in) {
        try{
            // Deserialize the Path
            int pathLength = in.readInt();
            System.out.println("Path length - " + pathLength);
            byte[] pathBytes = new byte[pathLength];
            in.readFully(pathBytes);
            Path path = Path.deserialize(pathBytes);

            // Deserialize the RTPpacket
            int packetLength = in.readInt();
            System.out.println("Packet length - " + packetLength);
            byte[] packetBytes = new byte[packetLength];
            in.readFully(packetBytes);
            RTPpacket packet = new RTPpacket(packetBytes, packetLength);

            return new FramePacket(path, packet);
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public Path getPath() {
        return this.path;
    }
}