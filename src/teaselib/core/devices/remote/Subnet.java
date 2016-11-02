/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Citizen Cane
 *
 */
@Deprecated
public class Subnet implements Iterable<InetAddress> {
    private final InterfaceAddress interfaceAddress;

    public static InetAddress subnetAddress(InterfaceAddress interfaceAddress)
            throws UnknownHostException {
        InetAddress address = interfaceAddress.getAddress();
        if (!(address instanceof Inet4Address)) {
            throw new IllegalArgumentException(interfaceAddress.toString());
        }
        byte[] bytes = address.getAddress();
        bytes[3] = 0;
        return InetAddress.getByAddress(bytes);
    }

    Subnet(InterfaceAddress interfaceAddress) {
        this.interfaceAddress = interfaceAddress;
    }

    @Override
    public Iterator<InetAddress> iterator() {
        if (!(interfaceAddress.getAddress() instanceof Inet4Address)) {
            throw new IllegalArgumentException(
                    "Only IP 4 networks are supported");
        }
        if (interfaceAddress.getNetworkPrefixLength() != 24) {
            throw new IllegalArgumentException(
                    "Only IP4 class C networks are supported");
        }
        final int start = 1;
        final int end = 254;
        List<InetAddress> subnetAddresses = new ArrayList<InetAddress>(
                end - start);
        byte[] ip = interfaceAddress.getAddress().getAddress();
        for (int i = start; i <= end; i++) {
            ip[3] = (byte) i;
            try {
                subnetAddresses.add(InetAddress.getByAddress(ip));
            } catch (UnknownHostException e) {
                // Ignored since there is no reverse lookup
            }
        }
        return subnetAddresses.iterator();
    }

    @Override
    public String toString() {
        try {
            return address().toString();
        } catch (UnknownHostException e) {
            return interfaceAddress.getAddress().toString();
        }
    }

    public InetAddress address() throws UnknownHostException {
        return subnetAddress(interfaceAddress);
    }

    /**
     * @return the interfaceAddress
     */
    public InetAddress getBroadcast() {
        return interfaceAddress.getBroadcast();
    }

}
