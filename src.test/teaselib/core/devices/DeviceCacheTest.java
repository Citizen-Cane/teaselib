package teaselib.core.devices;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.motiondetection.MotionDetector;

public class DeviceCacheTest {
    private static final Logger logger = LoggerFactory.getLogger(DeviceCacheTest.class);

    @Test
    public void testNaming() {
        String id = DeviceCache.createDevicePath("SomeDevice", "test");
        String name = DeviceCache.getDeviceName(id);
        assertEquals("test", name);
    }

    @Test
    public void testVideoCaptureDeviceEnumeration() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        Set<String> paths = devices.get(MotionDetector.class).getDevicePaths();
        for (String id : paths) {
            logger.info(id);
        }
    }
}
