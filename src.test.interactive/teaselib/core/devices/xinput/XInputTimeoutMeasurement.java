package teaselib.core.devices.xinput;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;

/**
 * @author Citizen-Cane
 *
 */
public class XInputTimeoutMeasurement {
    Logger logger = LoggerFactory.getLogger(XInputTimeoutMeasurement.class);

    @Test
    public void measureTimeoutDuration() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();

        XInputDevice xid = new Devices(config).get(XInputDevice.class).getDefaultDevice();
        DeviceCache.connect(xid);

        logger.info(xid.getDevicePath());
        BatteryLevel batteryLevel = xid.batteryLevel();
        logger.info("{} - battery {}", batteryLevel,
                (batteryLevel.isSuffcient() ? "is sufficient" : "needs recharging"));

        long start = System.currentTimeMillis();

        while (xid.connected()) {
            xid.poll();
            XInputComponentsDelta xic = xid.getDelta();
            XInputButtonsDelta xib = xic.getButtons();
            logger.info("{}", xib);
            Thread.sleep(60000);
        }

        long end = System.currentTimeMillis();

        logger.info("device {} timed out after {} minutes", xid.getDevicePath(), (end - start) / 1000 / 60);

        xid.close();
    }
}
