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
 * @author Citizen-Cane
 *
 */
public class StreamTests {

    private static byte[] testArray() {
        byte[] test = new byte[6666666];
        for (int i = 0; i < test.length; ++i) {
            test[i] = (byte) i;
        }
        return test;
    }

    @Test
    public void testCopy() throws IOException {
        byte[] test = testArray();
        testCopy(test);
    }

    @Test
    public void testSize() throws IOException {
        byte[] test = testArray();
        testSize(test, 5555555);
        testSize(test, 75434);
        testSize(test, 47);
    }

    public void testCopy(byte[] test) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(test);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Stream.copy(bis, bos);
        assertEquals(test.length, bos.toByteArray().length);
        assertArrayEquals(test, bos.toByteArray());
    }

    public void testSize(byte[] test, int s) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(test);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Stream.copy(bis, bos, s);
        assertEquals(s, bos.toByteArray().length);
    }
}
