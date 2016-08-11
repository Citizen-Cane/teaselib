package motiondetection;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.DeviceCache;
import teaselib.motiondetection.MotionDetection;

public class MotionDetectionTests {
    private static final Logger logger = LoggerFactory
            .getLogger(MotionDetectionTests.class);

    @Test
    public void testNaming() {
        String id = DeviceCache.createDevicePath("SomeDevice", "test");
        String name = DeviceCache.getDeviceName(id);
        assertEquals("test", name);
    }

    @Test
    public void testVideoCaptureDeviceEnumeration() {
        Set<String> devices = MotionDetection.Instance.getDevicePaths();
        for (String id : devices) {
            logger.info(id);
        }
    }
}
