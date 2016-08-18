/**
 * 
 */
package teaselib.core.devices;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.remote.KeyRelease;
import teaselib.core.devices.remote.RemoteDevice;
import teaselib.core.devices.remote.RemoteDevices;
import teaselib.core.devices.xinput.XInputDevices;
import teaselib.motiondetection.MotionDetection;
import teaselib.stimulation.StimulationDevices;
import teaselib.video.VideoCaptureDevices;

/**
 * @author someone
 *
 */
public class DeviceFactoryTests {
    private static final org.slf4j.Logger logger = LoggerFactory
            .getLogger(DeviceFactoryTests.class);

    @Test
    public void instanciateDeviceFactories() {
        logger.info("Available devices:");
        listDevices(XInputDevices.Instance.getDevicePaths());
        listDevices(StimulationDevices.Instance.getDevicePaths());
        listDevices(VideoCaptureDevices.Instance.getDevicePaths());
        listDevices(MotionDetection.Instance.getDevicePaths());
        listDevices(RemoteDevices.Instance.getDevicePaths());
        listDevices(KeyRelease.Devices.getDevicePaths());
    }

    private static void listDevices(Set<String> devicePaths) {
        assertNotNull(devicePaths);
        for (String devicePath : devicePaths) {
            logger.info(devicePath);
        }
    }

    @Test
    public void enumerateRemoteDevices() {
        final Set<String> devicePaths = RemoteDevices.Instance.getDevicePaths();
        assertNotNull(devicePaths);
        // assertNotNull(SelfBondageKeyRelease.Devices.getDevicePaths());
        for (String devicePath : devicePaths) {
            logger.info(devicePath);
            if (!Device.WaitingForConnection
                    .equals(DeviceCache.getDeviceName(devicePath))) {
                RemoteDevice device = RemoteDevices.Instance
                        .getDevice(devicePath);
                logger.info(
                        "-> " + device.getName() + ":" + device.getServiceName()
                                + " Version " + device.getVersion());
            }
        }
    }
}
