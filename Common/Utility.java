package Common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class Utility {

    public static Object deserializeObject(byte[] bytes) {
        InputStream is = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException ioe) {
            ioe.printStackTrace();
        }
        throw new RuntimeException();
    }

    public static int deserializeInt(byte[] bytes)
    {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return bb.getInt();
    }

    public static byte[] serializeObject(Object obj){
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try (ObjectOutputStream ois = new ObjectOutputStream(boas)) {
            ois.writeObject(obj);
            return boas.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        throw new RuntimeException();
    }

    public static byte[] serializeInt(int num)
    {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb = bb.putInt(num);
        return bb.array();
    }
    
}
