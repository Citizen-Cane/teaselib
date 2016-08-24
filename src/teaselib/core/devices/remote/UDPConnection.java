package teaselib.core.devices.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author
 *
 */
public class UDPConnection {
    private static final Logger logger = LoggerFactory
            .getLogger(UDPConnection.class);

    final InetAddress address;
    final int port;
    final DatagramSocket clientSocket;
    private int packetNumber = 0;

    public UDPConnection(InetAddress address, int port) throws SocketException {
        this.address = address;
        this.port = port;
        this.clientSocket = new DatagramSocket();
    }

    void close() {
        clientSocket.close();
    }

    boolean closed() {
        return clientSocket.isClosed();
    }

    public void send(String data) throws IOException {
        byte[] sendData = data.getBytes();
        send(sendData);
    }

    public void send(byte[] data) throws IOException {
        byte[] packetData = attachHeader(data);
        DatagramPacket packet = new DatagramPacket(packetData,
                packetData.length, address, port);
        send(packet);
    }

    private byte[] attachHeader(byte[] data) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        try {
            DataOutput output = new DataOutputStream(header);
            output.writeShort(++packetNumber);
            output.writeShort(data.length);
            output.write(data);
            return header.toByteArray();
        } finally {
            header.close();
        }
    }

    private void send(DatagramPacket sendPacket) throws IOException {
        clientSocket.send(sendPacket);
    }

    public byte[] sendAndReceive(String data, int timeoutMillis)
            throws IOException, SocketException {
        send(data);
        return receive(timeoutMillis);
    }

    public byte[] sendAndReceive(byte[] data, int timeoutMillis)
            throws IOException, SocketException {
        send(data);
        DatagramPacket receivePacket = receivePacket(timeoutMillis);
        return detachHeader(receivePacket.getData());
    }

    private byte[] receive(int timeoutMillis)
            throws SocketException, IOException {
        DatagramPacket receivePacket = receivePacket(timeoutMillis);
        return detachHeader(receivePacket.getData());
    }

    private byte[] detachHeader(byte[] data) throws IOException {
        ByteArrayInputStream rawData = new ByteArrayInputStream(data);
        try {
            DataInput input = new DataInputStream(rawData);
            int packetNumber = input.readShort();
            if (packetNumber < this.packetNumber) {
                logger.warn(
                        "Ignoring packet with smaller number #" + packetNumber);
            } else if (packetNumber > this.packetNumber) {
                // TODO better handling of packet number mismatch - wait if
                // smaller, throw if larger
                throw new IllegalStateException("Packet-Number mismatch");
            }
            int size = input.readShort();
            byte[] content = new byte[size];
            input.readFully(content);
            return content;
        } finally {
            rawData.close();
        }
    }

    private DatagramPacket receivePacket(int timeoutMillis)
            throws SocketException, IOException {
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                receiveData.length);
        clientSocket.setSoTimeout(timeoutMillis);
        clientSocket.receive(receivePacket);
        return receivePacket;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return address.getHostAddress() + ":" + port;
    }
}
