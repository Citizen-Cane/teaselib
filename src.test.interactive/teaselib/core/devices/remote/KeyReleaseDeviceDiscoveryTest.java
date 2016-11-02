/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen Cane
 *
 */
public class KeyReleaseDeviceDiscoveryTest {
    private static final Logger logger = LoggerFactory
            .getLogger(KeyReleaseDeviceDiscoveryTest.class);

    static final int Minutes = 2;

    @Test
    public void testBroadcastConnectProcedure() throws Exception {
        logger.info("Manual device discovery broadcast:");
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
    @Ignore
    public void testRudeNetworkScanConnectAndPotentiallyCrashNetwork()
            throws Exception {
        logger.info(
                "Network scan (may cause network trouble and wifi hotspot shutdown):");
        @SuppressWarnings("deprecation")
        LocalNetworkDeviceDiscoveryNetworkScan scanner = new LocalNetworkDeviceDiscoveryNetworkScan();
        collectFoundDevices(scanner);
    }

    @Test
    public void testLocalNetworkDeviceBroadcastDiscovery() throws Exception {
        logger.info("Local network device discovery broadcast):");
        LocalNetworkDeviceDiscoveryBroadcast scanner = new LocalNetworkDeviceDiscoveryBroadcast();
        collectFoundDevices(scanner);
    }

    private static void collectFoundDevices(LocalNetworkDeviceDiscovery scanner)
            throws InterruptedException {
        scanner.addRemoteDeviceDiscoveryListener(new RemoteDeviceListener() {
            @Override
            public void deviceAdded(String name, String address,
                    String serviceName, String description, String version) {
                logger.info(name + ":" + serviceName + ", " + description + ", "
                        + version + "@" + address);
            }
        });
        scanner.searchDevices();
        Thread.sleep(2000);
    }

    @Test
    public void testDeviceClass() throws Exception {
        logger.info(
                "Device factory network scan (uses broadcast device discoevry):");
        for (String devicePath : LocalNetworkDevice.Factory.getDevices()) {
            logger.info(devicePath);
        }
    }
}
