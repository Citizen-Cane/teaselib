/**
 * 
 */
package teaselib.devices.xinput;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author someone
 *
 */
public class XInputTest {

    @Test
    public void testEnumDevices() {
        XInputDevice[] devices = XInputDevice.getAllDevices();
        Assert.assertTrue(devices.length == 4);
        for (XInputDevice device : devices) {
            System.out.println("Device " + device.getPlayerNum() + ": "
                    + (device.isConnected() ? "connected" : "not connected"));
        }
    }
}
