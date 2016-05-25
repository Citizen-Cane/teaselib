/**
 * 
 */
package teaselib.stimulation;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.core.devices.xinput.XInputButtons;
import teaselib.core.devices.xinput.XInputComponents;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.core.devices.xinput.XInputDevices;

/**
 * @author someone
 *
 */
public class XInputTimeoutHackTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    public static Stimulator getLeftStimulator() {
        return StimulationDevices.Instance.getDefaultDevice().stimulators()
                .get(0);
    }

    public static Stimulator getRightStimulator() {
        return StimulationDevices.Instance.getDefaultDevice().stimulators()
                .get(1);
    }

    @Test
    public void testTimeoutHack_feedback() throws InterruptedException {
        XInputDevice xid = XInputDevices.Instance.getDefaultDevice();
        System.out.println(xid.getDevicePath());
        xid.setVibration((short) 0, (short) 0);
        XInputComponents xic;
        XInputButtons xib;
        try {
            Thread.sleep(1000);
            xid.poll();
            xic = xid.getComponents();
            xib = xic.getButtons();
            assertEquals(false, xib.lThumb);
            assertEquals(false, xib.rThumb);
            xid.setVibration(Short.MAX_VALUE, Short.MAX_VALUE);
            Thread.sleep(1000);
            xid.poll();
            xic = xid.getComponents();
            xib = xic.getButtons();
            // assertEquals(true, xib.lShoulder);
            // assertEquals(true, xib.rShoulder);
            assertEquals(true, xib.rThumb);
            assertEquals(true, xib.lThumb);
            xid.setVibration((short) 0, (short) 0);
            Thread.sleep(1000);
            xid.poll();
            xic = xid.getComponents();
            xib = xic.getButtons();
            assertEquals(false, xib.lThumb);
            assertEquals(false, xib.rThumb);
        } finally {
            xid.setVibration((short) 0, (short) 0);
        }
    }

    @Test
    @Ignore
    public void testTimeoutHack_1h() throws InterruptedException {
        XInputDevice xid = XInputDevices.Instance.getDefaultDevice();
        System.out.println(xid.getDevicePath());
        for (int i = 0; i < 360; i++) {
            xid.setVibration((short) 0, (short) 0);
            XInputComponents xic;
            XInputButtons xib;
            try {
                xid.poll();
                xic = xid.getComponents();
                xib = xic.getButtons();
                xid.setVibration(Short.MAX_VALUE, Short.MAX_VALUE);
                Thread.sleep(1000);
                xic = xid.getComponents();
                xib = xic.getButtons();
                xid.setVibration((short) 0, (short) 0);
                Thread.sleep(1000);
                xid.poll();
                xic = xid.getComponents();
                xib = xic.getButtons();
                assertEquals(false, xib.lThumb);
                assertEquals(false, xib.rThumb);
                Thread.sleep(8000);
            } finally {
                xid.setVibration((short) 0, (short) 0);
            }
        }
    }
}
