/**
 * 
 */
package teaselib.core.devices.xinput;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.junit.Test;

import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.StimulationDevices;

/**
 * @author someone
 *
 */
public class XInputTest {

    private static List<XInputDevice> getXInputDevices() {
        Collection<String> devicePaths = XInputDevices.Devices
                .getDevicePaths();
        List<XInputDevice> devices = new ArrayList<XInputDevice>(4);
        for (String devicePath : devicePaths) {
            XInputDevice xinputDevice = XInputDevices.Devices
                    .getDevice(devicePath);
            devices.add(xinputDevice);
        }
        return devices;
    }

    @Test
    public void testEnumXInputDevices() {
        List<XInputDevice> xinputDevices = getXInputDevices();
        for (XInputDevice xinputDevice : xinputDevices) {
            assertNotEquals(null, xinputDevice);
            assertEquals(true, xinputDevice.connected()
                    || xinputDevice.getPlayerNum() == 0);
            assertEquals(true,
                    xinputDevice.active() || xinputDevice.getPlayerNum() == 0);
            System.out.println("Device " + xinputDevice.getDevicePath() + ": "
                    + (xinputDevice.connected() ? "connected"
                            : "not connected"));
        }
    }

    @Test
    public void testEnumStimulationDevices() {
        List<XInputDevice> xinputDevices = getXInputDevices();
        int n = 0;
        for (XInputDevice xinputDevice : xinputDevices) {
            final boolean useDevice = xinputDevice.connected()
                    || xinputDevice.getPlayerNum() == 0;
            assertEquals(true, useDevice);
            if (useDevice) {
                n++;
            }
        }
        Collection<String> devicePaths = StimulationDevices.Devices
                .getDevicePaths();
        List<StimulationDevice> stimulationDevices = new Vector<StimulationDevice>();
        for (String devicePath : devicePaths) {
            StimulationDevice stimulationDevice = StimulationDevices.Devices
                    .getDevice(devicePath);
            if (stimulationDevice instanceof XInputStimulationDevice) {
                stimulationDevices.add(stimulationDevice);
            }
        }
        assertEquals(n, stimulationDevices.size());
        for (StimulationDevice stimulationDevice : stimulationDevices) {
            if (stimulationDevice instanceof XInputStimulationDevice) {
                XInputStimulationDevice xinputStimulationDevice = (XInputStimulationDevice) stimulationDevice;
                assertNotEquals(null, stimulationDevice);
                System.out.println(
                        "Device " + xinputStimulationDevice.getDevicePath()
                                + ": " + (xinputStimulationDevice.connected()
                                        ? "connected" : "not connected"));
            }
        }
    }
}
