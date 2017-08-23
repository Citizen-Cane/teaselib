/**
 * 
 */
package teaselib.core.devices.xinput;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;
import teaselib.stimulation.StimulationDevice;
import teaselib.test.DebugSetup;

/**
 * @author someone
 *
 */
public class XInputTest {

    private static List<XInputDevice> getXInputDevices(Devices devices) {
        DeviceCache<XInputDevice> deviceCache = devices.get(XInputDevice.class);
        Collection<String> devicePaths = deviceCache.getDevicePaths();
        List<XInputDevice> xinputDevices = new ArrayList<XInputDevice>(4);
        for (String devicePath : devicePaths) {
            XInputDevice xinputDevice = deviceCache.getDevice(devicePath);
            xinputDevices.add(xinputDevice);
        }
        return xinputDevices;
    }

    @Test
    public void testEnumXInputDevices() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        List<XInputDevice> xinputDevices = getXInputDevices(devices);
        for (XInputDevice xinputDevice : xinputDevices) {
            assertNotEquals(null, xinputDevice);
            assertEquals(true, xinputDevice.connected() || xinputDevice.getPlayerNum() == 0);
            assertEquals(true, xinputDevice.active() || xinputDevice.getPlayerNum() == 0);
            System.out.println("Device " + xinputDevice.getDevicePath() + ": "
                    + (xinputDevice.connected() ? "connected" : "not connected"));
        }
    }

    @Test
    public void testEnumStimulationDevices() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        List<XInputDevice> xinputDevices = getXInputDevices(devices);
        int n = 0;
        for (XInputDevice xinputDevice : xinputDevices) {
            final boolean useDevice = xinputDevice.connected() || xinputDevice.getPlayerNum() == 0;
            assertEquals(true, useDevice);
            if (useDevice) {
                n++;
            }
        }
        Collection<String> devicePaths = devices.get(StimulationDevice.class).getDevicePaths();
        List<StimulationDevice> stimulationDevices = new Vector<StimulationDevice>();
        for (String devicePath : devicePaths) {
            StimulationDevice stimulationDevice = devices.get(StimulationDevice.class).getDevice(devicePath);
            if (stimulationDevice instanceof XInputStimulationDevice) {
                stimulationDevices.add(stimulationDevice);
            }
        }
        assertEquals(n, stimulationDevices.size());

        for (StimulationDevice stimulationDevice : stimulationDevices) {
            if (stimulationDevice instanceof XInputStimulationDevice) {
                XInputStimulationDevice xinputStimulationDevice = (XInputStimulationDevice) stimulationDevice;
                assertNotEquals(null, stimulationDevice);
                System.out.println("Device " + xinputStimulationDevice.getDevicePath() + ": "
                        + (xinputStimulationDevice.connected() ? "connected" : "not connected"));
            }
        }
    }
}
