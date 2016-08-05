package motiondetection;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
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
        TeaseLib teaseLib = TeaseLib.init(new DummyHost(),
                new DummyPersistence());
        Set<String> devices = MotionDetection.Instance.getDevicePaths();
        for (String id : devices) {
            logger.info(id);
        }
    }
}
