/**
 * 
 */
package teaselib.core.devices;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.core.devices.xinput.XInputDevices;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;
import teaselib.motiondetection.MotionDetection;
import teaselib.stimulation.StimulationDevices;
import teaselib.video.VideoCaptureDevices;

/**
 * @author someone
 *
 */
public class DeviceFactoryTests {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TeaseLib.init(new DummyHost(), new DummyPersistence());
    }

    @Test
    public void instanciateAll() {
        assertNotNull(XInputDevices.Instance.getDevicePaths());
        assertNotNull(StimulationDevices.Instance.getDevicePaths());
        assertNotNull(VideoCaptureDevices.Instance.getDevicePaths());
        assertNotNull(MotionDetection.Instance.getDevicePaths());
        assertNotNull(RemoteDevices.Instance.getDevicePaths());
        assertNotNull(SelfBondageKeyRelease.Devices.getDevicePaths());
    }

    @Test
    public void enumerateRemoteDevices() {
        final Set<String> devicePaths = RemoteDevices.Instance.getDevicePaths();
        assertNotNull(devicePaths);
        // assertNotNull(SelfBondageKeyRelease.Devices.getDevicePaths());
        for (String devicePath : devicePaths) {
            TeaseLib.instance().log.info(devicePath);
            RemoteDevice device = RemoteDevices.Instance.getDevice(devicePath);
            TeaseLib.instance().log.info(
                    "-> " + device.getName() + ":" + device.getServiceName()
                            + " Version " + device.getVersion());

        }
    }
}
