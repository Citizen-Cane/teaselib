/**
 * 
 */
package teaselib.core.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

/**
 * @author someone
 *
 */
public class StreamTests {

    @Test
    public void testCopySize() throws IOException {
        byte[] test = new byte[6666666];
        testCopy(test);
        testCopySize(test, 5555555);
        testCopySize(test, 75434);
        testCopySize(test, 47);
    }

    public void testCopy(byte[] test) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(test);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Stream.copy(bis, bos);
        assertEquals(test.length, bos.toByteArray().length);
    }

    public void testCopySize(byte[] test, int s) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(test);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Stream.copy(bis, bos, s);
        assertEquals(s, bos.toByteArray().length);
    }
}
