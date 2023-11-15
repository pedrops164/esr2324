package Common;

import java.nio.ByteBuffer;

public class IPAddress {
    private int[] ipAdrr;

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

    public IPAddress (int[] values)
    {
        this.ipAdrr = values.clone();
    }

    public byte[] serialize()
    {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        for (int num : ipAdrr)
            buffer.putInt(num);
        
        return buffer.array();
    }

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
