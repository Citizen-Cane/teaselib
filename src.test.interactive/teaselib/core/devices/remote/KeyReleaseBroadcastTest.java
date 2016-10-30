/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetAddress;
import java.net.InterfaceAddress;
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
        for (InterfaceAddress interfaceAddress : new LocalNetworkDeviceDiscoveryBroadcast()
                .networks()) {
            logger.info("Sending broadcast message to "
                    + interfaceAddress.getBroadcast().toString());
            UDPConnection connection = new UDPConnection(
                    interfaceAddress.getBroadcast(), 666);
            connection.send(new UDPMessage(RemoteDevice.Id).toByteArray());
            while (true) {
                try {
                    byte[] received = connection.receive(1000);
                    UDPMessage udpMessage = new UDPMessage(received);
                    RemoteDeviceMessage device = udpMessage.message;
                    if ("services".equals(device.command)) {
                        devices.put(
                                InetAddress.getByName(device.parameters.get(1)),
                                device);
                    }
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
        new LocalNetworkDeviceDiscoveryNetworkScan().searchDevices(deviceCache);
        for (Map.Entry<String, LocalNetworkDevice> device : deviceCache
                .entrySet()) {
            logger.info(device.getKey().toString() + ", "
                    + device.getValue().getDescription() + ", "
                    + device.getValue().getVersion());
        }
    }

    @Test
    public void testBroadcastDiscovery() throws Exception {
        Map<String, LocalNetworkDevice> deviceCache = new LinkedHashMap<String, LocalNetworkDevice>();
        new LocalNetworkDeviceDiscoveryBroadcast().searchDevices(deviceCache);
        for (Map.Entry<String, LocalNetworkDevice> device : deviceCache
                .entrySet()) {
            logger.info(device.getKey().toString() + ", "
                    + device.getValue().getDescription() + ", "
                    + device.getValue().getVersion());
        }
    }
}
