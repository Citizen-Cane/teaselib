package motiondetection;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.motiondetection.MotionDetectorFactory;

public class MotionDetectionTests {

    @Test
    public void testNaming() {
        String id = MotionDetectorFactory.makeId(MotionDetectionTests.class,
                "test");
        String name = MotionDetectorFactory.getName(id);
        assertEquals("test", name);
    }

    @Test
    public void testVideoCaptureDeviceEnumeration() {
        TeaseLib teaseLib = TeaseLib.init(new DummyHost(),
                new DummyPersistence());
        Set<String> devices = MotionDetectorFactory.getDevices();
        for (String id : devices) {
            teaseLib.log.info(id);
        }
    }
}
