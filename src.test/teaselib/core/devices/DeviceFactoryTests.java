/**
 * 
 */
package teaselib.core.devices;

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.remote.RemoteDevice;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.motiondetection.MotionDetector;
import teaselib.stimulation.StimulationDevice;
import teaselib.test.DebugSetup;
import teaselib.video.VideoCaptureDevice;

/**
 * @author someone
 *
 */
public class DeviceFactoryTests {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceFactoryTests.class);

    @Test
    public void instanciateDeviceFactories() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        logger.info("Available devices:");
        listDevices(devices.get(XInputDevice.class).getDevicePaths());
        listDevices(devices.get(StimulationDevice.class).getDevicePaths());
        listDevices(devices.get(VideoCaptureDevice.class).getDevicePaths());
        listDevices(devices.get(MotionDetector.class).getDevicePaths());
        listDevices(devices.get(RemoteDevice.class).getDevicePaths());
        listDevices(devices.get(KeyRelease.class).getDevicePaths());
    }

    private static void listDevices(Set<String> devicePaths) {
        assertNotNull(devicePaths);
        for (String devicePath : devicePaths) {
            logger.info(devicePath);
        }
    }

    @Test
    public void enumerateRemoteDevices() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        DeviceCache<RemoteDevice> deviceCache = devices.get(RemoteDevice.class);

        Set<String> devicePaths = deviceCache.getDevicePaths();
        assertNotNull(devicePaths);

        for (String devicePath : devicePaths) {
            logger.info(devicePath);
            if (!Device.WaitingForConnection.equals(DeviceCache.getDeviceName(devicePath))) {
                RemoteDevice device = deviceCache.getDevice(devicePath);
                logger.info(
                        "-> " + device.getName() + ":" + device.getServiceName() + " Version " + device.getVersion());
            }
        }
    }
}
