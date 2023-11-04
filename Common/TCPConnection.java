package Common;

import java.io.*;
import java.util.*;
import java.net.*;

// This class contains all the information for every type of management TCP packet 
// The possible types are the following:
/*
 * 1. Server communicates to RP his available videos.
*/


// Nota: Estou a pensar em implementar o conteúdo do pacote de outra maneira

public class TCPConnection {
    // Communication
    private Socket s; 
    private DataInputStream in;
    private DataOutputStream out;

    public static class Packet{
        public int type;
        public byte[] data;

        public Packet(int type, byte[] data){
            this.type = type;
            this.data = data;
        }
    }

    public TCPConnection(Socket s){
        this.s = s;
        try{
            InputStream in = s.getInputStream();
            this.in = new DataInputStream(in);

            OutputStream out = s.getOutputStream();
            this.out = new DataOutputStream(out);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void send(Packet p){
        try{
            this.out.writeInt(p.type);
            this.out.writeInt(p.data.length);
            this.out.write(p.data);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void send(int type, byte[] data){
        try{
            this.out.writeInt(type);
            this.out.writeInt(data.length);
            this.out.write(data);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Packet receive(){
        int tag = -1;
        byte[] data = null;

        try {
            tag = this.in.readInt();
            int comp = this.in.readInt();
            data = new byte[comp];
            this.in.readFully(data);
        }catch (Exception e){
            e.printStackTrace();
        }

        return new Packet(tag, data);
    }

    public void stopConnection(){
        try{
            this.s.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
