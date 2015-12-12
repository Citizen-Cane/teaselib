/**
 * 
 */
package teaselib.core.util;

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
}
