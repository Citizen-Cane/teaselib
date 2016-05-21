/**
 * 
 */
package teaselib.core.devices.xinput;

import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.Test;

import teaselib.core.devices.xinput.stimulation.XInputStimulationDevice;
import teaselib.stimulation.StimulationDevice;
import teaselib.stimulation.StimulationDevices;

/**
 * @author someone
 *
 */
public class XInputTest {

    @Test
    public void testEnumStimulationDevices() {
        Collection<String> devicePaths = StimulationDevices.Instance
                .getDevices();
        assertTrue(devicePaths.size() == 4);
        for (String devicePath : devicePaths) {
            StimulationDevice stimulationDevice = StimulationDevices.Instance
                    .getDevice(devicePath);
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
