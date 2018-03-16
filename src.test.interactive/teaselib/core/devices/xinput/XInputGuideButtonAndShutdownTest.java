/**
 * 
 */
package teaselib.core.devices.xinput;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;

public class XInputGuideButtonAndShutdownTest {
    private static final Logger logger = LoggerFactory.getLogger(XInputGuideButtonAndShutdownTest.class);

    @Test
    public void testPressGuideButtonFor1SecondAndShutdown() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        XInputDevice xid = devices.get(XInputDevice.class).getDefaultDevice();
        assertTrue(xid.getDevicePath() + " not connected", xid.connected());

        logger.info(xid.getDevicePath());
        try {
            // Press guide button for one seconds
            while (true) {
                xid.poll();
                XInputComponentsDelta xic = xid.getDelta();
                XInputButtonsDelta xib = xic.getButtons();
                logger.info(xib.toString());
                if (!xib.isPressed(XInputButton.guide)) {
                    Thread.sleep(100);
                    continue;
                }
                Thread.sleep(1000);
                xid.poll();
                logger.info(xib.toString());
                if (xib.isReleased(XInputButton.guide)) {
                    Thread.sleep(100);
                    continue;
                }
                break;
            }
            // Wait until guide button is released
            xid.setVibration(XInputDevice.VIBRATION_MAX_VALUE, XInputDevice.VIBRATION_MAX_VALUE);
            while (true) {
                xid.poll();
                XInputComponentsDelta xic = xid.getDelta();
                XInputButtonsDelta xib = xic.getButtons();
                logger.info(xib.toString());
                if (!xib.isReleased(XInputButton.guide)) {
                    Thread.sleep(100);
                    continue;
                }
                break;
            }
            logger.info("Guide button pressed for one second - shutting down controller");
        } finally {
            assertTrue(xid.shutdown());
        }
    }
}
