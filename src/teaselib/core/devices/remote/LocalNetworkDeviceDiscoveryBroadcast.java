/**
 * 
 */
package teaselib.core.devices.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LocalNetworkDeviceDiscoveryBroadcast extends LocalNetworkDeviceDiscovery {
    static final Logger logger = LoggerFactory
            .getLogger(LocalNetworkDeviceDiscoveryBroadcast.class);

    private final Map<InterfaceAddress, BroadcastListener> discoveryThreads = new HashMap<InterfaceAddress, BroadcastListener>();
    private BroadcastListener globalBroadcastListener;

    public LocalNetworkDeviceDiscoveryBroadcast() {
        super();

        installBroadcastListener();
    }

    private void installBroadcastListener() {
        try {
            globalBroadcastListener = new BroadcastListener();
            globalBroadcastListener.start();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    void searchDevices() throws InterruptedException {
        try {
            List<InterfaceAddress> networks = networks();
            updateInterfaceBroadcastListeners(networks);
            broadcastToAllInterfaces(networks);
        } catch (SocketException e) {
            logger.error(e.getMessage(), e);
        }
        waitForDevicesToReply();
    }

    void updateInterfaceBroadcastListeners(List<InterfaceAddress> networks) {
        if (globalBroadcastListener == null) {
            installBroadcastListener();
        }
        addSocketThreadsForNewNetworkInterfaces(networks);
        removeSocketThreadsForVanishedNetworks(networks);
    }

    private void addSocketThreadsForNewNetworkInterfaces(
            List<InterfaceAddress> networks) {
        for (InterfaceAddress interfaceAddress : networks) {
            if (!discoveryThreads.containsKey(interfaceAddress)) {
                try {
                    BroadcastListener socketThread = new BroadcastListener(
                            interfaceAddress.getBroadcast(),
                            LocalNetworkDevice.Port);
                    discoveryThreads.put(interfaceAddress, socketThread);
                    socketThread.start();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private void removeSocketThreadsForVanishedNetworks(
            List<InterfaceAddress> networks) {
        for (Entry<InterfaceAddress, BroadcastListener> entry : discoveryThreads
                .entrySet()) {
            if (!networks.contains(entry.getKey())) {
                discoveryThreads.remove(entry.getKey()).interrupt();
            }
        }
    }

    private void broadcastToAllInterfaces(List<InterfaceAddress> networks) {
        for (InterfaceAddress intefaceAddess : networks) {
            try {
                sendBroadcastIDMessage(intefaceAddess);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void sendBroadcastIDMessage(InterfaceAddress interfaceAddress)
            throws IOException {
        InetAddress broadcastAddress = interfaceAddress.getBroadcast();
        logger.info(
                "Sending broadcast message to " + broadcastAddress.toString());
        UDPConnection connection = discoveryThreads
                .get(interfaceAddress).connection;
        connection.send(new UDPMessage(RemoteDevice.Id).toByteArray());
    }

    private static void waitForDevicesToReply() throws InterruptedException {
        Thread.sleep(2000);
    }

    class BroadcastListener extends Thread {
        final UDPConnection connection;
        private final AtomicBoolean isRunning = new AtomicBoolean(false);

        public BroadcastListener(InetAddress address, int port)
                throws IOException {
            connection = new UDPConnection(address, port);
            initThread();
            setName("Installed interface broadcast socket on "
                    + address.toString());
        }

        BroadcastListener() throws IOException {
            InetSocketAddress address = new InetSocketAddress(
                    InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }),
                    LocalNetworkDevice.Port);
            connection = new UDPConnection(address);
            connection.setCheckPacketNumber(false);
            initThread();
            setName("Listening for broadcast packets on " + address.toString());
        }

        private void initThread() {
            setDaemon(true);
        }

        @Override
        public synchronized void start() {
            super.start();
            isRunning.set(true);
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    try {
                        byte[] received = connection.receive();
                        RemoteDeviceMessage services = new UDPMessage(
                                received).message;
                        if ("services".equals(services.command)) {
                            logger.info("Received device startup message on "
                                    + connection);
                            fireDeviceDiscovered(services);
                        }
                    } catch (SocketException e) {
                        boolean socketHasBeenClosed = isRunning.get();
                        if (socketHasBeenClosed) {
                            logger.error(e.getMessage(), e);
                        }
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } finally {
                connection.close();
            }
        }

        @Override
        public void interrupt() {
            isRunning.set(false);
            super.interrupt();
            connection.close();
        }
    }

    @Override
    void close() {
        if (globalBroadcastListener != null) {
            globalBroadcastListener.interrupt();
            try {
                globalBroadcastListener.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
