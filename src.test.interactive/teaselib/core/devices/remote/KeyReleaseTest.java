/**
 * 
 */
package teaselib.core.devices.remote;

import static org.junit.Assert.*;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

/**
 * @author someone
 *
 */
public class KeyReleaseTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
    }

    static final int minutes = 2;

    @Test
    public void test() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        int actuators = keyRelease.actuators();
        TeaseLib.instance().log
                .info(keyRelease.getName() + ": " + actuators + " actuators");
        assertTrue(actuators > 0);
        for (int i = 0; i < actuators; i++) {
            int available = keyRelease.available(i);
            assertTrue(available > 0);
            keyRelease.arm(i);
            keyRelease.start(i, minutes);
            assertTrue(keyRelease.isRunning(i));
            TeaseLib.instance().log.info("Actuator " + i);
            while (keyRelease.isRunning(i)) {
                int remaining = keyRelease.remaining(i);
                if (remaining == 0) {
                    break;
                }
                try {
                    Thread.sleep(6 * 1000);
                } catch (InterruptedException e) {
                    Assume.assumeTrue(false);
                }
            }
            // Release the key in the last minute
            keyRelease.release(i);
            assertFalse(keyRelease.isRunning(i));
        }
    }
}
