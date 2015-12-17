/**
 * 
 */
package teaselib.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author someone
 *
 */
public class Stream {
    public static void copy(InputStream is, OutputStream os) throws IOException {
        int i;
        int size = 1024 * 1024;
        byte[] b = new byte[size];
        while ((i = is.read(b, 0, size)) != -1) {
            os.write(b, 0, i);
        }
    }

    public static void copy(InputStream is, OutputStream os, long s)
            throws IOException {
        int i;
        int bufferSize = 1024 * 1024;
        byte[] b = new byte[bufferSize];
        while ((i = is.read(b, 0, Math.min(bufferSize, (int) s))) != -1) {
            s -= i;
            os.write(b, 0, i);
            if (s == 0) {
                break;
            }
        }
    }

    public static boolean sameContent(InputStream is1, InputStream is2)
            throws IOException {
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        copy(is1, bos1);
        copy(is2, bos2);
        bos1.close();
        bos2.close();
        final byte[] ba1 = bos1.toByteArray();
        final byte[] ba2 = bos2.toByteArray();
        if (ba1.length != ba2.length) {
            return false;
        }
        for (int i = 0; i < ba1.length; i++) {
            if (ba1[i] != ba2[i]) {
                return false;
            }
        }
        return true;
    }
}
