package Common;

import java.util.HashMap;

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

public class UDPDatagram{

    private static int header_size = 3; // Header size -> 3 integers
    // Header fields
    private int payloadType; // Type of the content being streamed
    private int sequenceNumber; // Number of the present frame being streamed
    private int timeStamp; // Corresponding timestamp of the video being played (in ms)
    
    // Content of the stream
    private byte[] payload;
    private int payload_size;

    public UDPDatagram(int pt, int sn, int ts, byte[] payload, int payload_size){
        this.payloadType = pt;
        this.sequenceNumber = sn;
        this.timeStamp = ts;
        this.payload = new byte[payload_size];

        // Initialize payload array
        for(int i=0; i<payload_size; i++){
            this.payload[i] = payload[i];
        }
        this.payload_size = payload_size;
    } 

    public UDPDatagram(byte[] data, int packet_size){
        if(packet_size > header_size){
            // First we have 3 integers of header   
            this.payloadType = data[0];
            this.sequenceNumber = data[1];
            this.timeStamp = data[2];

            // Then we have the payload
            this.payload = new byte[packet_size - header_size];
            for(int i=header_size; i<packet_size; i++){
                this.payload[i- header_size] = data[i];
            }
            this.payload_size = packet_size - header_size;
        }else{
            System.out.println("Invalid UDP Datagram!");
        }
    }

    public int datagramSize(){
        return(header_size + this.payload_size);
    }

    public void getDatagram(byte[] data){
        data[0] = (byte) this.payloadType;
        data[1] = (byte) this.sequenceNumber;
        data[2] = (byte) this.timeStamp;
        
        for(int i=0; i<this.payload_size; i++){
            data[i+3] = this.payload[i];
        }
    }

    public void printDatagramHeader(){
        HashMap<Integer, String> types = new HashMap<>();
        types.put(0, "Mjepg");
        types.put(1, "MP4");
        types.put(2, "Mov");

        System.out.println("Payload Type: " + types.get(this.payloadType));
        System.out.println("Frame Number: " + this.sequenceNumber);
        System.out.println("Time Stamp: " + this.timeStamp);
    }
}