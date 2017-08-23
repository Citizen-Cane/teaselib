/**
 * 
 */
package teaselib.core.devices.xinput;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;

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

        logger.info(xid.getDevicePath());
        BatteryLevel batteryLevel = xid.batteryLevel();
        logger.info(batteryLevel.toString() + (batteryLevel.isSuffcient() ? " is sufficient" : " needs recharging"));

        xid.close();
    }

}
