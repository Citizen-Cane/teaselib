package teaselib.core.devices.remote;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalNetworkDeviceTests {
    private static final Logger logger = LoggerFactory.getLogger(LocalNetworkDeviceTests.class);

    @Test
    public void testResourceDeallocationQuiteLong() throws Exception {
        System.setProperty(LocalNetworkDevice.EnableDeviceDiscovery, Boolean.TRUE.toString());

        int j = 10;
        testResourceDeallocation(j);
    }

    private static void testResourceDeallocation(int j) throws Exception {
        for (int i = 0; i < j; i++) {
            try {
                List<String> devices = LocalNetworkDevice.Factory.getDevices();
                for (String device : devices) {
                    logger.info("Found device: " + device);
                }
            } catch (Exception e) {
                logger.info(
                        "Enumerating network devices failed after " + i + " iterations. Reason: ",
                        e.getMessage());
                throw e;
            }
        }
    }

    @Test
    public void ensureConfigSettingChangeIsDetected() throws Exception {
        assertEquals("teaselib.core.devices.remote.LocalNetworkDevice.EnableDeviceDiscovery",
                LocalNetworkDevice.EnableDeviceDiscovery);
        assertEquals("teaselib.core.devices.remote.LocalNetworkDevice.EnableDeviceStatusListener",
                LocalNetworkDevice.EnableDeviceStatusListener);
    }
}
