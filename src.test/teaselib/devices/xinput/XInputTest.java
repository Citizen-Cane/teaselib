/**
 * 
 */
package teaselib.devices.xinput;

import org.junit.Assert;
import org.junit.Test;

import teaselib.devices.xinput.XInputDevice;
import teaselib.util.jni.LibraryLoader;

/**
 * @author someone
 *
 */
public class XInputTest {

    @Test
    public void test() {
        LibraryLoader.load("TeaseLibx360c");
        // retrieve all devices
        XInputDevice[] devices = XInputDevice.getAllDevices();
        Assert.assertTrue(devices.length == 4);
        for (XInputDevice device : devices) {
            System.out.println("Device " + device.getPlayerNum() + ": "
                    + (device.isConnected() ? "connected" : "not connected"));
        }
    }
}
