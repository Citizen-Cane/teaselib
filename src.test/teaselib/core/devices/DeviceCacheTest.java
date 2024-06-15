package teaselib.core.devices;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Citizen-Cane
 */
public class DeviceCacheTest {

    @Test
    public void testNaming() {
        String id = DeviceCache.createDevicePath("SomeDevice", "test");
        String name = DeviceCache.getDeviceName(id);
        assertEquals("test", name);
    }

}
