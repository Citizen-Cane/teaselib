/**
 * 
 */
package teaselib.stimulation;

import static org.junit.Assert.*;

import org.junit.Ignore;
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
public class XInputTimeoutHackTest {

    @Test
    public void testTimeoutHack_feedback() throws InterruptedException {
        XInputDevice xid = XInputDevices.Instance.getDefaultDevice();
        System.out.println(xid.getDevicePath());
        xid.setVibration((short) 0, (short) 0);
        XInputButtonsDelta xib;
        try {
            xib = poll(xid);
            assertEquals(false, xib.isPressed(XInputButton.lShoulder));
            assertEquals(false, xib.isPressed(XInputButton.rShoulder));
            xid.setVibration(Short.MAX_VALUE, Short.MAX_VALUE);
            xib = poll(xid);
            assertEquals(true, xib.isPressed(XInputButton.lShoulder));
            assertEquals(true, xib.isPressed(XInputButton.rShoulder));
            xid.setVibration((short) 0, (short) 0);
            xib = poll(xid);
            assertEquals(false, xib.isPressed(XInputButton.lShoulder));
            assertEquals(false, xib.isPressed(XInputButton.rShoulder));
        } finally {
            assertTrue(xid.shutdown());
        }
    }

    @Test
    @Ignore
    public void testTimeoutHack_1h() throws InterruptedException {
        XInputDevice xid = XInputDevices.Instance.getDefaultDevice();
        System.out.println(xid.getDevicePath());
        for (int i = 0; i < 360; i++) {
            xid.setVibration((short) 0, (short) 0);
            XInputButtonsDelta xib;
            try {
                xid.poll();
                xid.setVibration(XInputDevice.VIBRATION_MAX_VALUE,
                        XInputDevice.VIBRATION_MAX_VALUE);
                xib = poll(xid);
                assertEquals(true, xib.isPressed(XInputButton.lShoulder));
                assertEquals(true, xib.isPressed(XInputButton.rShoulder));
                xid.setVibration(XInputDevice.VIBRATION_MAX_VALUE,
                        XInputDevice.VIBRATION_MIN_VALUE);
                xib = poll(xid);
                assertEquals(true, xib.isPressed(XInputButton.lShoulder));
                assertEquals(false, xib.isPressed(XInputButton.rShoulder));
                xid.setVibration(XInputDevice.VIBRATION_MIN_VALUE,
                        XInputDevice.VIBRATION_MAX_VALUE);
                xib = poll(xid);
                assertEquals(false, xib.isPressed(XInputButton.lShoulder));
                assertEquals(true, xib.isPressed(XInputButton.rShoulder));
                xid.setVibration(XInputDevice.VIBRATION_MIN_VALUE,
                        XInputDevice.VIBRATION_MIN_VALUE);
                xib = poll(xid);
                assertEquals(false, xib.isPressed(XInputButton.lShoulder));
                assertEquals(false, xib.isPressed(XInputButton.rShoulder));
            } finally {
                assertTrue(xid.shutdown());
            }
        }
    }

    private static XInputButtonsDelta poll(XInputDevice xid)
            throws InterruptedException {
        Thread.sleep(100);
        xid.poll();
        XInputComponentsDelta xic = xid.getDelta();
        XInputButtonsDelta xib = xic.getButtons();
        System.out.println(xib);
        return xib;
    }
}
