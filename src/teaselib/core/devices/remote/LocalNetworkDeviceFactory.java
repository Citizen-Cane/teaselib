package teaselib.core.devices.remote;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;

// TODO change device enumeration on and off to work at runtime
// - currently it's only initialized at startup to force firewall notifications popping up
// while the user is able to interact - however this is always the case when preparing a device

// TODO Change device status listening on and off to work at runtime - initialized at startup only
// - in LocalNetworkDeviceDiscovery(Broadcast) install/remove status handler when adding/removing listeners
// - warn if listeners are added while configuration setting is disabled
// - execute and actually install broadcast packet listners when setting is turned on
// to make sure that firewall notification dialogs popup when expected

public final class LocalNetworkDeviceFactory extends DeviceFactory<LocalNetworkDevice> {
    private static final Logger logger = LoggerFactory.getLogger(LocalNetworkDeviceFactory.class);

    private final LocalNetworkDeviceDiscovery deviceDiscovery;

    private final List<LocalNetworkDevice> discoveredDevices = new ArrayList<>();

    public LocalNetworkDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
        super(deviceClass, devices, configuration);

        deviceDiscovery = new LocalNetworkDeviceDiscoveryBroadcast();
        installDeviceDiscoveryListener();

        if (isDeviceDiscoveryEnabled()) {
            deviceDiscovery.enableDeviceStatusListener(isListeningForDeviceMessagesEnabled());
            startDeviceDetection();
        }
    }

    private void installDeviceDiscoveryListener() {
        deviceDiscovery.addRemoteDeviceDiscoveryListener((name, address, serviceName, description, version) -> {
            String devicePath = LocalNetworkDevice.createDevicePath(name, serviceName);
            if (!isDeviceCached(devicePath)) {
                try {
                    LocalNetworkDevice device = new LocalNetworkDevice(name, new UDPConnection(address), serviceName,
                            description, version);
                    synchronized (discoveredDevices) {
                        discoveredDevices.add(device);
                    }
                } catch (NumberFormatException e1) {
                    LocalNetworkDevice.logger.error(e1.getMessage(), e1);
                } catch (SocketException e2) {
                    LocalNetworkDevice.logger.error(e2.getMessage(), e2);
                } catch (UnknownHostException e3) {
                    LocalNetworkDevice.logger.error(e3.getMessage(), e3);
                }
            }
        });
    }

    @Override
    public List<String> enumerateDevicePaths(Map<String, LocalNetworkDevice> deviceCache) throws InterruptedException {
        if (discoveredDevices.isEmpty() && isDeviceDiscoveryEnabled()) {
            deviceDiscovery.searchDevices();
        } else if (!isDeviceDiscoveryEnabled()) {
            logger.warn("Network device discovery disabled - devices will not be detected");
        }
        publishDiscoveredDevices(deviceCache);
        return new ArrayList<>(deviceCache.keySet());
    }

    private void publishDiscoveredDevices(Map<String, LocalNetworkDevice> deviceCache) {
        synchronized (discoveredDevices) {
            for (LocalNetworkDevice device : discoveredDevices) {
                deviceCache.put(device.getDevicePath(), device);
            }
            discoveredDevices.clear();
        }
    }

    @Override
    public LocalNetworkDevice createDevice(String deviceName) {
        if (Device.WaitingForConnection.equals(deviceName)) {
            throw new IllegalArgumentException(Device.WaitingForConnection);
        }
        String[] deviceAndAddress = deviceName.split("@");
        String[] deviceInfo = deviceAndAddress[0].split(",");
        String name = deviceInfo[0];
        String serviceName = deviceInfo[1];
        String description = deviceInfo[2];
        String version = deviceInfo[3];
        String address = deviceAndAddress[1];
        LocalNetworkDevice localNetworkDevice;
        try {
            localNetworkDevice = new LocalNetworkDevice(name, new UDPConnection(address), serviceName, description,
                    version);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        } catch (SocketException e) {
            throw new IllegalArgumentException(e);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
        return localNetworkDevice;
    }

    public boolean isListeningForDeviceMessagesEnabled() {
        return Boolean.parseBoolean(configuration.get(LocalNetworkDevice.Settings.EnableDeviceStatusListener));
    }

    public boolean isDeviceDiscoveryEnabled() {
        return Boolean.parseBoolean(configuration.get(LocalNetworkDevice.Settings.EnableDeviceDiscovery));
    }

    public void startDeviceDetection() {
        // TODO Remove dependency to Swing
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getDevices();
            }
        });
    }
}