package Common;

import java.nio.ByteBuffer;

/**
 * Class used to represent an IPv4 address on a {@link PathNode}, 
 * to make serialization easier
 */
public class IPAddress {
    /**
     * int array containing the 4 digits of an ip address
     */
    private int[] ipAdrr;

    /**
     * Constructor that receives a string representing an
     * IPv4 Address
     * 
     * @param IPAddr string representing an IPv4 Address
     */
    public IPAddress (String IPAddr)
    {
        this.ipAdrr = new int[4];
        String[] sep = IPAddr.split("[.]");
        for (int i=0 ; i<4 ; i++)
        {
            int num = Integer.parseInt(sep[i]);
            this.ipAdrr[i] = num;
        }
    }

    /**
     * Constructor that receives an int[] containing all 4
     * integers on a IPv4 address
     * 
     * @param values int[] containing all 4 integers on a 
     * IPv4 address
     */
    public IPAddress (int[] values)
    {
        this.ipAdrr = values.clone();
    }

    /**
     * Function used to serialize an IPAdress
     * 
     * @return byte[] containing the 16 bytes of the serialized 
     * integers
     */
    public byte[] serialize()
    {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        for (int num : ipAdrr)
            buffer.putInt(num);
        
        return buffer.array();
    }

    /**
     * Function used to deserialize an IPAdress
     * 
     * @param arr byte[] containing a serialized IPAddress
     * @return deserialized IPAddress
     */
    public static IPAddress deserialize(byte[] arr)
    {
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        int[] values = new int[4];

        for (int i=0 ; i<4 ; i++)
            values[i] = buffer.getInt(i*4);
        
        return new IPAddress(values);
    }

    @Override
    public String toString()
    {
        return ipAdrr[0] + "." + ipAdrr[1] + "." + ipAdrr[2] + "." + ipAdrr[3];
    }
}
