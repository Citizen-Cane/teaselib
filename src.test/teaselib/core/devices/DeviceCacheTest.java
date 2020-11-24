package teaselib.core.devices;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Citizen-Cane
 *
 */
public class DeviceCacheTest {

    @Test
    public void testNaming() {
        String id = DeviceCache.createDevicePath("SomeDevice", "test");
        String name = DeviceCache.getDeviceName(id);
        assertEquals("test", name);
    }

}
