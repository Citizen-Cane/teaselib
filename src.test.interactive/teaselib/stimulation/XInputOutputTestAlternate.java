/**
 * 
 */
package teaselib.stimulation;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.core.devices.xinput.XInputAxes;
import teaselib.core.devices.xinput.XInputButtons;
import teaselib.core.devices.xinput.XInputComponents;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.core.devices.xinput.XInputDevices;

/**
 * @author someone
 *
 */
public class XInputOutputTestAlternate {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void testTimeoutHack_1h() throws InterruptedException {
        XInputDevice xid = XInputDevices.Instance.getDefaultDevice();
        System.out.println(xid.getDevicePath());
        for (int i = 0; i < 1000; i++) {
            try {
                testVibration(xid, (short) 65535, (short) 0);
                testVibration(xid, (short) 0, (short) 0);
                testVibration(xid, (short) 0, (short) 65535);
            } finally {
                testVibration(xid, (short) 0, (short) 0);
            }
        }
    }

    private static void testVibration(XInputDevice xid, short left, short right)
            throws InterruptedException {
        XInputComponents xic;
        XInputButtons xib;
        XInputAxes xia;
        xid.setVibration(left, right);
        Thread.sleep(500);
        xid.poll();
        xic = xid.getComponents();
        xib = xic.getButtons();
        xia = xic.getAxes();
        System.out.println(
                xia.lt + ", " + xia.rt + ", " + xib.lThumb + ", " + xib.rThumb);
        Thread.sleep(3500);
    }
}
