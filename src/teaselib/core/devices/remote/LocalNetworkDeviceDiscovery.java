/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

abstract class LocalNetworkDeviceDiscovery {
    abstract void searchDevices(Map<String, LocalNetworkDevice> deviceCache)
            throws InterruptedException;

    List<InterfaceAddress> networks() throws SocketException {
        List<InterfaceAddress> interfaceAddresses = new ArrayList<InterfaceAddress>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                .getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(networkInterfaces)) {
            if (netint.isUp() && !netint.isVirtual() && !netint.isLoopback()) {
                for (InterfaceAddress interfaceAddress : netint
                        .getInterfaceAddresses()) {
                    if (interfaceAddress.getAddress() instanceof Inet4Address) {
                        interfaceAddresses.add(interfaceAddress);
                    }
                }
            }
        }
        return interfaceAddresses;
    }
}