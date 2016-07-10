package motiondetection;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.motiondetection.MotionDetection;

public class MotionDetectionTests {

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
            teaseLib.log.info(id);
        }
    }
}
