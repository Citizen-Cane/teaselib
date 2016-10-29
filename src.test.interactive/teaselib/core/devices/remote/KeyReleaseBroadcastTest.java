/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author someone
 *
 */
public class KeyReleaseBroadcastTest {
    private static final Logger logger = LoggerFactory
            .getLogger(KeyReleaseBroadcastTest.class);

    static final int Minutes = 2;

    @Test
    public void testBroadcastConnect() throws Exception {
        Map<InetAddress, RemoteDeviceMessage> devices = new HashMap<InetAddress, RemoteDeviceMessage>();
        for (Subnet subnet : LocalNetworkDevice.enumerateNetworks()) {
            InetAddress broadcastAddress = subnet.getBroadcast();
            logger.info("Sending broasdcast message to "
                    + broadcastAddress.toString());
            UDPConnection connection = new UDPConnection(broadcastAddress, 666);
            connection.send(new UDPMessage(RemoteDevice.Id).toByteArray());
            while (true) {
                try {
                    byte[] received = connection.receive(1000);
                    UDPMessage udpMessage = new UDPMessage(received);
                    RemoteDeviceMessage device = udpMessage.message;
                    devices.put(InetAddress.getByName(device.parameters.get(1)),
                            device);
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
        }
        for (Map.Entry<InetAddress, RemoteDeviceMessage> device : devices
                .entrySet()) {
            logger.info(
                    device.getKey().toString() + " -> " + device.getValue());
        }
    }

    @Test
    public void testNetworkScanConnect() throws Exception {
        Map<String, LocalNetworkDevice> deviceCache = new LinkedHashMap<String, LocalNetworkDevice>();
        LocalNetworkDevice.searchDevicesWithNetworkScan(deviceCache);
        for (Map.Entry<String, LocalNetworkDevice> device : deviceCache
                .entrySet()) {
            logger.info(device.getKey().toString() + ", "
                    + device.getValue().getDescription() + ", "
                    + device.getValue().getVersion());
        }
    }
}
