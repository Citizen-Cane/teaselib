package motiondetection;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.motiondetection.MotionDetector;
import teaselib.test.DebugSetup;

public class MotionDetectionTests {
    private static final Logger logger = LoggerFactory.getLogger(MotionDetectionTests.class);

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
