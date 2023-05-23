package teaselib.core.devices.remote;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import teaselib.core.Closeable;

public abstract class LocalNetworkDeviceDiscovery implements Closeable {

    final List<RemoteDeviceListener> remoteDeviceListeners = new ArrayList<>();

    public abstract void enableDeviceStatusListener(boolean enable);

    abstract void searchDevices() throws InterruptedException;

    @Override
    public abstract void close();

    List<InterfaceAddress> networks() throws SocketException {
        List<InterfaceAddress> interfaceAddresses = new ArrayList<>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(networkInterfaces)) {
            if (netint.isUp() && !netint.isVirtual() && !netint.isLoopback()) {
                for (InterfaceAddress interfaceAddress : netint.getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress() instanceof Inet4Address) {
                        interfaceAddresses.add(interfaceAddress);
                    }
                }
            }
        }
        return interfaceAddresses;
    }

    protected void fireDeviceDiscovered(RemoteDeviceMessage services) throws UnknownHostException {
        int i = 0;
        String name = services.parameters.get(i++);
        InetAddress address = InetAddress.getByName(services.parameters.get(i++));
        int serviceCount = Integer.parseInt(services.parameters.get(i++));
        for (int j = 0; j < serviceCount; j++) {
            String serviceName = services.parameters.get(i++);
            String description = services.parameters.get(i++);
            String version = services.parameters.get(i++);
            fireDeviceDiscovered(name, address.getHostAddress() + ":" + LocalNetworkDevice.Port, serviceName,
                    description, version);
        }
    }

    protected void fireDeviceDiscovered(String name, String address, String serviceName, String description,
            String version) {
        for (RemoteDeviceListener remoteDeviceListener : remoteDeviceListeners) {
            remoteDeviceListener.deviceAdded(name, address, serviceName, description, version);
        }
    }

    public void addRemoteDeviceDiscoveryListener(RemoteDeviceListener remoteDeviceListener) {
        remoteDeviceListeners.add(remoteDeviceListener);
    }

    public void removeRemoteDeviceDiscoveryListener(RemoteDeviceListener remoteDeviceListener) {
        remoteDeviceListeners.remove(remoteDeviceListener);
    }

}