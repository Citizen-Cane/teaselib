/**
 * 
 */
package teaselib.core.devices.xinput;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;

/**
 * @author Citizen-Cane
 *
 */
public class XInputTimeoutHackTest {
    Logger logger = LoggerFactory.getLogger(XInputTimeoutHackTest.class);

    @Test
    public void testTimeoutHack_feedback() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        XInputDevice xid = devices.get(XInputDevice.class).getDefaultDevice();
        DeviceCache.connect(xid);

        logger.info(xid.getDevicePath());
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
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        XInputDevice xid = devices.get(XInputDevice.class).getDefaultDevice();
        System.out.println(xid.getDevicePath());
        for (int i = 0; i < 360; i++) {
            xid.setVibration((short) 0, (short) 0);
            XInputButtonsDelta xib;
            try {
                xid.poll();
                xid.setVibration(XInputDevice.VIBRATION_MAX_VALUE, XInputDevice.VIBRATION_MAX_VALUE);
                xib = poll(xid);
                assertEquals(true, xib.isPressed(XInputButton.lShoulder));
                assertEquals(true, xib.isPressed(XInputButton.rShoulder));
                xid.setVibration(XInputDevice.VIBRATION_MAX_VALUE, XInputDevice.VIBRATION_MIN_VALUE);
                xib = poll(xid);
                assertEquals(true, xib.isPressed(XInputButton.lShoulder));
                assertEquals(false, xib.isPressed(XInputButton.rShoulder));
                xid.setVibration(XInputDevice.VIBRATION_MIN_VALUE, XInputDevice.VIBRATION_MAX_VALUE);
                xib = poll(xid);
                assertEquals(false, xib.isPressed(XInputButton.lShoulder));
                assertEquals(true, xib.isPressed(XInputButton.rShoulder));
                xid.setVibration(XInputDevice.VIBRATION_MIN_VALUE, XInputDevice.VIBRATION_MIN_VALUE);
                xib = poll(xid);
                assertEquals(false, xib.isPressed(XInputButton.lShoulder));
                assertEquals(false, xib.isPressed(XInputButton.rShoulder));
            } finally {
                assertTrue(xid.shutdown());
            }
        }
    }

    private static XInputButtonsDelta poll(XInputDevice xid) throws InterruptedException {
        Thread.sleep(100);
        xid.poll();
        XInputComponentsDelta xic = xid.getDelta();
        XInputButtonsDelta xib = xic.getButtons();
        System.out.println(xib);
        return xib;
    }
}
