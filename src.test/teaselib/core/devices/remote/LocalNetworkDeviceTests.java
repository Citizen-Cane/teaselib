package teaselib.core.devices.remote;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalNetworkDeviceTests {
    private static final Logger logger = LoggerFactory
            .getLogger(LocalNetworkDeviceTests.class);

    @Test
    @Ignore
    public void testResourceDeallocationQuiteLong() throws Exception {
        for (int i = 0; i < 1000; i++) {
            try {
                LocalNetworkDevice.Factory.getDevices();
            } catch (Exception e) {
                logger.info("Enumerating network devices failed after " + i
                        + " iterations. Reason: ", e.getMessage());
                throw e;
            }
        }
    }
}
