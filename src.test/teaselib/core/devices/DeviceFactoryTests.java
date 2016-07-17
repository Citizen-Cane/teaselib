/**
 * 
 */
package teaselib.core.devices;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.TeaseLib;
import teaselib.core.devices.remote.RemoteDevice;
import teaselib.core.devices.remote.RemoteDevices;
import teaselib.core.devices.remote.SelfBondageKeyRelease;
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
    public void instanciateDeviceFactories() {
        TeaseLib.instance().log.info("Available devices:");
        listDevices(XInputDevices.Instance.getDevicePaths());
        listDevices(StimulationDevices.Instance.getDevicePaths());
        listDevices(VideoCaptureDevices.Instance.getDevicePaths());
        listDevices(MotionDetection.Instance.getDevicePaths());
        listDevices(RemoteDevices.Instance.getDevicePaths());
        listDevices(SelfBondageKeyRelease.Devices.getDevicePaths());
    }

    private void listDevices(Set<String> devicePaths) {
        assertNotNull(devicePaths);
        for (String devicePath : devicePaths) {
            TeaseLib.instance().log.info(devicePath);
        }
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
