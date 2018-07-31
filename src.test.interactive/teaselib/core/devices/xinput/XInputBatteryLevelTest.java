package teaselib.core.devices.xinput;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Devices;

/**
 * @author Citizen-Cane
 *
 */
public class XInputBatteryLevelTest {
    Logger logger = LoggerFactory.getLogger(XInputBatteryLevelTest.class);

    @Test
    public void testBatteryLevel() {
        Configuration config = DebugSetup.getConfiguration();

        XInputDevice xid = new Devices(config).get(XInputDevice.class).getDefaultDevice();
        assertTrue(xid.getDevicePath() + " not connected", xid.connected());

        logger.info(xid.getDevicePath());
        BatteryLevel batteryLevel = xid.batteryLevel();
        logger.info("{} - battery {}", batteryLevel,
                (batteryLevel.isSuffcient() ? "is sufficient" : "needs recharging"));

        xid.close();
    }

}
