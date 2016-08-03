/**
 * 
 */
package teaselib.core.devices.remote;

import static org.junit.Assert.*;

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

    @Test
    public void test() {
        KeyRelease keyRelease = KeyRelease.Devices.getDefaultDevice();
        TeaseLib.instance().log.info("-> " + keyRelease.getName() + ": "
                + keyRelease.actuators() + " actuators");
        assertTrue(keyRelease.actuators() > 0);
    }
}
