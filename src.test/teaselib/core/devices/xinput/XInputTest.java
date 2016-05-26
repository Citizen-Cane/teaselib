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

    private List<XInputDevice> getXInputDevices() {
        Collection<String> devicePaths = XInputDevices.Instance.getDevices();
        List<XInputDevice> devices = new ArrayList<XInputDevice>(4);
        for (String devicePath : devicePaths) {
            XInputDevice xinputDevice = XInputDevices.Instance
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
            assertEquals(true, xinputDevice.isConnected());
            System.out.println("Device " + xinputDevice.getDevicePath() + ": "
                    + (xinputDevice.isConnected() ? "connected"
                            : "not connected"));
        }
    }

    @Test
    public void testEnumStimulationDevices() {
        List<XInputDevice> xinputDevices = getXInputDevices();
        int n = 0;
        for (XInputDevice xinputDevice : xinputDevices) {
            assertEquals(true, xinputDevice.isConnected());
            if (xinputDevice.isConnected()) {
                n++;
            }
        }
        Collection<String> devicePaths = StimulationDevices.Instance
                .getDevices();
        List<StimulationDevice> stimulationDevices = new Vector<StimulationDevice>();
        for (String devicePath : devicePaths) {
            StimulationDevice stimulationDevice = StimulationDevices.Instance
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
                                + ": " + (xinputStimulationDevice.isConnected()
                                        ? "connected" : "not connected"));
            }
        }
    }
}
