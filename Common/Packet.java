package Common;

import java.io.*;
// Classe que contém a informação dos pacotes de gestão de tráfego (enviadas por TCP)

public class Packet {
    private int type; 

    public Packet(int type){
        this.type = type;
    }

    public void serialize (DataOutputStream out){
        try{
            // write the content of the packet to the socket here
            out.writeInt(this.type);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Packet deserialize (DataInputStream in){
        int type = -1;
        try{
            // read the content of the packet from the socket here
            type = in.readInt();
        }catch (Exception e){
            e.printStackTrace();
        }
        return new Packet(type);
    }
}
