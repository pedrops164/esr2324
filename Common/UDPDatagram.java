package Common;

import java.io.*;

/*
 * Class that contains all the information present in a UDP Datagram
 * 
 * Note: I think it would be good to make the payloadType an enum
 */

/*
 * Payload type (add if needed):
 *  Mjpeg: 0 
 *  MP4: 1
 *  Mov: 2  
 */

public class UDPDatagram implements Comparable<UDPDatagram>, Serializable {

    //private static int header_size = 4; // Header size -> 4 integers
    // Header fields
    private int payloadType; // Type of the content being streamed
    private int sequenceNumber; // Number of the present frame being streamed
    private int timeStamp; // Corresponding timestamp of the video being played (in ms)
    private int framePeriod; // inverse of fps - represents the time between frames being displayed on the client screen
    private String streamName;
    
    // Content of the stream
    private byte[] payload;
    private int payload_size;

    public UDPDatagram(int pt, int sn, int ts, byte[] payload, int payload_size, int framePeriod, String streamName){
        this.payloadType = pt;
        this.sequenceNumber = sn;
        this.timeStamp = ts;
        this.streamName = streamName;
        this.framePeriod = framePeriod;
        this.payload = new byte[payload_size];

        // Initialize payload array
        for(int i=0; i<payload_size; i++){
            this.payload[i] = payload[i];
        }
        this.payload_size = payload_size;
    } 

    public byte[] getPayload() {
        byte[] ret = new byte[this.payload_size];
        for(int i=0; i<this.payload_size; i++){
            ret[i] = this.payload[i];
        }
        return ret;
    }

    public int getPayloadLength() {
        return this.payload_size;
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

    public static UDPDatagram deserialize(byte[] receivedBytes) throws IOException {
        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
            ObjectInput in = new ObjectInputStream(bais);

            UDPDatagram ret = (UDPDatagram) in.readObject();
            
            bais.close();
            in.close();
            
            return ret;

        } catch (StreamCorruptedException e){
            throw e;
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public int getTimeStamp() {
        return this.timeStamp;
    }
    
    public int getFramePeriod() {
        return this.framePeriod;
    }

    public String getStreamName() {
        return this.streamName;
    }

    @Override
    public int compareTo(UDPDatagram other) {
        return Integer.compare(this.timeStamp, other.timeStamp);
    }
}
