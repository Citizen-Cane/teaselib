package teaselib.core.devices.remote;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.core.util.QualifiedItem;
import teaselib.test.DebugSetup;

public class LocalNetworkDeviceTests {
    private static final Logger logger = LoggerFactory.getLogger(LocalNetworkDeviceTests.class);

    @Test
    public void testResourceDeallocationQuiteLong() {
        Configuration config = DebugSetup.getConfigurationWithRemoteDeviceAccess();
        Devices devices = new Devices(config);

        int j = 10;
        testResourceDeallocation(devices, j);
    }

    private static void testResourceDeallocation(Devices devices, int j) {
        for (int i = 0; i < j; i++) {
            try {
                for (String device : devices.get(LocalNetworkDevice.class).getDevicePaths()) {
                    logger.info("Found device: {}", device);
                }
            } catch (Exception e) {
                logger.error("Enumerating network devices failed after {} iterations because of {}", i, e.getMessage());
                throw e;
            }
        }
    }

    @Test
    public void ensureConfigSettingNameChangeIsDetected() {
        assertEquals("teaselib.core.devices.remote.LocalNetworkDevice.Settings.EnableDeviceDiscovery",
                QualifiedItem.of(LocalNetworkDevice.Settings.EnableDeviceDiscovery).toString());
        assertEquals("teaselib.core.devices.remote.LocalNetworkDevice.Settings.EnableDeviceStatusListener",
                QualifiedItem.of(LocalNetworkDevice.Settings.EnableDeviceStatusListener).toString());
    }
}
