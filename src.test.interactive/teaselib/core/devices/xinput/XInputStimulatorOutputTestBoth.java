package teaselib.core.devices.xinput;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;

public class XInputStimulatorOutputTestBoth {
    static final Logger logger = LoggerFactory.getLogger(XInputStimulatorOutputTestBoth.class);

    @Test
    public void testAlternate() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        XInputDevice xid = devices.get(XInputDevice.class).getDefaultDevice();
        logger.info("{}{}", xid.getDevicePath(), (xid.connected() ? "" : ":" + Device.WaitingForConnection));
        DeviceCache.connect(xid);

        try {
            for (int i = 0; i < 1000; i++) {
                testVibration(xid, XInputDevice.VIBRATION_MAX_VALUE, XInputDevice.VIBRATION_MAX_VALUE);
                testVibration(xid, XInputDevice.VIBRATION_MIN_VALUE, XInputDevice.VIBRATION_MIN_VALUE);
            }
        } finally {
            testVibration(xid, XInputDevice.VIBRATION_MIN_VALUE, XInputDevice.VIBRATION_MIN_VALUE);
        }
    }

    private static void testVibration(XInputDevice xid, int left, int right) throws InterruptedException {
        XInputComponents xic;
        XInputButtons xib;
        XInputAxes xia;
        xid.setVibration(left, right);
        Thread.sleep(2500);
        xid.poll();
        xic = xid.getComponents();
        xib = xic.getButtons();
        xia = xic.getAxes();
        logger.info("{}, {}, {}, {}", xia.lt, xia.rt, xib.lShoulder, xib.rShoulder);
        Thread.sleep(2500);
    }
}
