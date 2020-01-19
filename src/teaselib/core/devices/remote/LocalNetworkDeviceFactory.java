package teaselib.core.devices.remote;

import static teaselib.core.util.ExceptionUtil.asRuntimeException;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;

// TODO change device enumeration on and off to work at runtime
// - currently it's only initialized at startup to force firewall notifications popping up
// while the user is able to interact - however this is always the case when preparing a device

// TODO Change device status listening on and off to work at runtime - initialized at startup only
// - in LocalNetworkDeviceDiscovery(Broadcast) install/remove status handler when adding/removing listeners
// - warn if listeners are added while configuration setting is disabled
// - execute and actually install broadcast packet listeners when setting is turned on
// to make sure that firewall notification dialogs pop up when expected

public final class LocalNetworkDeviceFactory extends DeviceFactory<LocalNetworkDevice> {
    private final LocalNetworkDeviceDiscovery deviceDiscovery;

    public LocalNetworkDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
        super(deviceClass, devices, configuration);

        deviceDiscovery = new LocalNetworkDeviceDiscoveryBroadcast();
        installDeviceDiscoveryListener();

        if (isDeviceDiscoveryEnabled()) {
            deviceDiscovery.enableDeviceStatusListener(isListeningForDeviceMessagesEnabled());
            try {
                deviceDiscovery.searchDevices();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void installDeviceDiscoveryListener() {
        deviceDiscovery.addRemoteDeviceDiscoveryListener((name, address, serviceName, description, version) -> {
            String devicePath = LocalNetworkDevice.createDevicePath(name, serviceName);
            if (!isDeviceCached(devicePath)) {
                deviceDiscovered(devicePath,
                        () -> createDeviceInstance(name, serviceName, description, version, address));
            }
            fireDeviceConnected(devicePath, LocalNetworkDevice.class);
        });

    }

    private LocalNetworkDevice createDeviceInstance(String name, String serviceName, String description, String version,
            String address) {
        LocalNetworkDevice localNetworkDevice;
        try {
            localNetworkDevice = new LocalNetworkDevice(this, name, address, serviceName, description, version);
        } catch (NumberFormatException | UnknownHostException e) {
            throw new IllegalArgumentException(e);
        } catch (SocketException e) {
            throw asRuntimeException(e);
        }
        return localNetworkDevice;
    }

    @Override
    public List<String> enumerateDevicePaths(Map<String, LocalNetworkDevice> deviceCache) throws InterruptedException {
        return new ArrayList<>(deviceCache.keySet());
    }

    @Override
    public LocalNetworkDevice createDevice(String deviceName) {
        throw new UnsupportedOperationException("Local network devices can be created via device discovery only");
    }

    public boolean isListeningForDeviceMessagesEnabled() {
        return Boolean.parseBoolean(configuration.get(LocalNetworkDevice.Settings.EnableDeviceStatusListener));
    }

    public boolean isDeviceDiscoveryEnabled() {
        return Boolean.parseBoolean(configuration.get(LocalNetworkDevice.Settings.EnableDeviceDiscovery));
    }

}