/**
 * 
 */
package teaselib.stimulation;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.core.devices.xinput.XInputButton;
import teaselib.core.devices.xinput.XInputButtonsDelta;
import teaselib.core.devices.xinput.XInputComponentsDelta;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.core.devices.xinput.XInputDevices;

/**
 * @author someone
 *
 */
public class XInputGuideButtonAndShutdown {

    @Test
    public void testPressGuideButtonFor1SecondAndShutdown()
            throws InterruptedException {
        XInputDevice xid = XInputDevices.Instance.getDefaultDevice();
        System.out.println(xid.getDevicePath());
        try {
            // Press guide button for one seconds
            while (true) {
                xid.poll();
                XInputComponentsDelta xic = xid.getDelta();
                XInputButtonsDelta xib = xic.getButtons();
                System.out.println(xib);
                if (!xib.isPressed(XInputButton.guide)) {
                    Thread.sleep(100);
                    continue;
                }
                Thread.sleep(1000);
                xid.poll();
                System.out.println(xib);
                if (xib.isReleased(XInputButton.guide)) {
                    Thread.sleep(100);
                    continue;
                }
                break;
            }
            // Wait until guide button is released
            xid.setVibration(XInputDevice.VIBRATION_MAX_VALUE,
                    XInputDevice.VIBRATION_MAX_VALUE);
            while (true) {
                xid.poll();
                XInputComponentsDelta xic = xid.getDelta();
                XInputButtonsDelta xib = xic.getButtons();
                System.out.println(xib);
                if (!xib.isReleased(XInputButton.guide)) {
                    Thread.sleep(100);
                    continue;
                }
                break;
            }
            System.out.println(
                    "Guide button pressed for one second - shutting down controller");
        } finally {
            // Shutdown
            assertTrue(xid.shutdown());
        }
    }
}
