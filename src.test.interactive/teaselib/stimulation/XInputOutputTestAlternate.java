package teaselib.stimulation;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.core.devices.xinput.XInputAxes;
import teaselib.core.devices.xinput.XInputButtons;
import teaselib.core.devices.xinput.XInputComponents;
import teaselib.core.devices.xinput.XInputDevice;
import teaselib.core.devices.xinput.XInputDevices;

public class XInputOutputTestAlternate {

    @BeforeClass
    public static void setUpBeforeClass() {
    }

    @Test
    public void testAlternate() throws InterruptedException {
        XInputDevice xid = XInputDevices.Instance.getDefaultDevice();
        System.out.println(xid.getDevicePath());
        for (int i = 0; i < 1000; i++) {
            try {
                testVibration(xid, XInputDevice.VIBRATION_MAX_VALUE,
                        XInputDevice.VIBRATION_MIN_VALUE);
                testVibration(xid, XInputDevice.VIBRATION_MIN_VALUE,
                        XInputDevice.VIBRATION_MAX_VALUE);
                testVibration(xid, XInputDevice.VIBRATION_MIN_VALUE,
                        XInputDevice.VIBRATION_MIN_VALUE);
                testVibration(xid, XInputDevice.VIBRATION_MAX_VALUE,
                        XInputDevice.VIBRATION_MAX_VALUE);
            } finally {
                testVibration(xid, XInputDevice.VIBRATION_MIN_VALUE,
                        XInputDevice.VIBRATION_MIN_VALUE);
            }
        }
    }

    private static void testVibration(XInputDevice xid, int left, int right)
            throws InterruptedException {
        XInputComponents xic;
        XInputButtons xib;
        XInputAxes xia;
        xid.setVibration(left, right);
        Thread.sleep(2500);
        xid.poll();
        xic = xid.getComponents();
        xib = xic.getButtons();
        xia = xic.getAxes();
        System.out.println(xia.lt + ", " + xia.rt + ", " + xib.lShoulder + ", "
                + xib.rShoulder);
        Thread.sleep(2500);
    }
}
