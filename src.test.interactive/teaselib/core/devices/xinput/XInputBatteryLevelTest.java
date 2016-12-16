/**
 * 
 */
package teaselib.core.devices.xinput;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.BatteryLevel;

/**
 * @author someone
 *
 */
public class XInputBatteryLevelTest {
    Logger logger = LoggerFactory.getLogger(XInputBatteryLevelTest.class);

    @Test
    public void testBatteryLevel() {
        XInputDevice xid = XInputDevices.Devices.getDefaultDevice();
        logger.info(xid.getDevicePath());
        BatteryLevel batteryLevel = xid.batteryLevel();
        logger.info(batteryLevel.toString() + (batteryLevel.isSuffcient()
                ? " is sufficient" : " needs recharging"));
        xid.close();
    }

}
