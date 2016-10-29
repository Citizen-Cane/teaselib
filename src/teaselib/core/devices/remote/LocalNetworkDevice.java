/**
 * 
 */
package teaselib.core.devices.remote;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;

/**
 * @author Citizen-Cane
 *
 */
public class LocalNetworkDevice implements RemoteDevice {
    static final Logger logger = LoggerFactory
            .getLogger(LocalNetworkDevice.class);

    private static final String DeviceClassName = "LocalNetworkDevice";

    static final int Port = 666;

    /**
     * Local network devices have to respond in this time, plus some head room.
     */
    public static final int AllowedTimeoutMillis = 1000;

    /**
     * The socket timeout, after which a packet is considered timed out.
     */
    private static final int SocketTimeoutMillis = 2000;

    public static final DeviceFactory<LocalNetworkDevice> Factory = new DeviceFactory<LocalNetworkDevice>(
            DeviceClassName) {

        private LocalNetworkDeviceDiscoveryNetworkScan deviceDiscovery = new LocalNetworkDeviceDiscoveryNetworkScan();

        @Override
        public List<String> enumerateDevicePaths(
                Map<String, LocalNetworkDevice> deviceCache)
                throws InterruptedException {
            deviceDiscovery.searchDevices(deviceCache);
            return new ArrayList<String>(deviceCache.keySet());
        }

        @Override
        public LocalNetworkDevice createDevice(String deviceName) {
            if (WaitingForConnection.equals(deviceName)) {
                throw new IllegalArgumentException(WaitingForConnection);
            }
            throw new UnsupportedOperationException(deviceName);
        }

    };

    private final String name;
    private final String serviceName;
    private final String description;
    private final String version;

    private UDPConnection connection;

    LocalNetworkDevice(String name, UDPConnection connection,
            String serviceName, String description, String version) {
        this.name = name;
        this.connection = connection;
        this.serviceName = serviceName;
        this.description = description;
        this.version = version;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                name + "@" + connection.toString() + "->" + serviceName);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean connected() {
        boolean connected = !connection.closed();
        if (connected) {
            RemoteDeviceMessage ok;
            try {
                ok = sendAndReceive(Id, SocketTimeoutMillis);
            } catch (SocketException e) {
                ok = Timeout;
            } catch (IOException e) {
                ok = Timeout;
            }
            if (ok.equals(Timeout)) {
                reconnect();
                try {
                    ok = sendAndReceive(Id, SocketTimeoutMillis);
                } catch (SocketException e) {
                    ok = Timeout;
                } catch (IOException e) {
                    ok = Timeout;
                }
                connected = ok != Timeout;
            }
        }
        return connected;
    }

    @Override
    public boolean active() {
        return !connection.closed();
    }

    @Override
    public void close() {
        connection.close();
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public int sleep(int timeMinutes) {
        RemoteDeviceMessage count = sendAndReceive(new RemoteDeviceMessage(
                Sleep, Arrays.asList(Integer.toString(timeMinutes))));
        if (Count.equals(count.command)) {
            return Integer.parseInt(count.parameters.get(0));
        } else {
            return 0;
        }
    }

    @Override
    public RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message) {
        RemoteDeviceMessage received = Timeout;
        for (int i = 0; i < 3; i++) {
            try {
                received = sendAndReceive(message, SocketTimeoutMillis);
                break;
            } catch (SocketException e) {
                continue;
            } catch (IOException e) {
                continue;
            }
        }
        if (received == Timeout) {
            reconnect();
            for (int i = 0; i < 3; i++) {
                try {
                    received = sendAndReceive(message, SocketTimeoutMillis);
                    break;
                } catch (SocketException e) {
                    continue;
                } catch (IOException e) {
                    continue;
                }
            }
        }
        return received;
    }

    private RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message,
            int timeout) throws SocketException, IOException {
        logger.info("Sending " + message.toString());
        byte[] received = connection
                .sendAndReceive(new UDPMessage(message).toByteArray(), timeout);
        RemoteDeviceMessage receivedMessage = new UDPMessage(received).message;
        logger.info("Received " + receivedMessage.toString());
        return receivedMessage;
    }

    private void reconnect() {
        Factory.removeDisconnectedDevice(this);
        List<String> devicePaths = Factory.getDevices();
        for (String devicePath : devicePaths) {
            if (!WaitingForConnection.equals(devicePath)
                    && getDevicePath().equals(devicePath)) {
                LocalNetworkDevice device = Factory.getDevice(devicePath);
                connection.close();
                connection = device.connection;
                Factory.connectDevice(this);
                break;
            }
        }
    }

    @Override
    public void send(RemoteDeviceMessage message) {
        try {
            logger.info("Sending " + message.getClass().getSimpleName() + ": "
                    + message.toString());
            connection.send(new UDPMessage(message).toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
