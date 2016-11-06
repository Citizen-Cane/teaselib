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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author
 *
 */
public class UDPConnection {
    private static final int PacketBufferSize = 1024;

    private static final Logger logger = LoggerFactory
            .getLogger(UDPConnection.class);

    private static final int SequenceNumberForIncomingBroadcastPacket = 0;
    final InetAddress address;
    final int port;
    final DatagramSocket clientSocket;
    private int packetNumber = SequenceNumberForIncomingBroadcastPacket;

    private boolean checkPacketNumber = true;

    public UDPConnection(String address) throws SocketException,
            NumberFormatException, UnknownHostException {
        this(InetAddress.getByName(address.split(":")[0]),
                Integer.parseInt(address.split(":")[1]));
    }

    public UDPConnection(InetAddress address, int port) throws SocketException {
        this.address = address;
        this.port = port;
        this.clientSocket = new DatagramSocket();
    }

    // TODO Make clear that the socket is bound to the given address - the
    // initial idea was that the adress denotes the target
    public UDPConnection(InetSocketAddress address) throws IOException {
        this.address = address.getAddress();
        this.port = address.getPort();
        this.clientSocket = new DatagramSocket(address);
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
            output.writeShort(getPacketNumber());
            output.writeShort(data.length);
            output.write(data);
            return header.toByteArray();
        } finally {
            header.close();
        }
    }

    private int getPacketNumber() {
        packetNumber++;
        packetNumber &= 0x7fff;
        if (packetNumber == SequenceNumberForIncomingBroadcastPacket) {
            packetNumber++;
        }
        return packetNumber;
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
        clientSocket.setSoTimeout(timeoutMillis);
        return receiveMatchingPacket();
    }

    public byte[] receive() throws SocketException, IOException {
        clientSocket.setSoTimeout(0);
        return receiveMatchingPacket();
    }

    public byte[] receive(int timeoutMillis)
            throws SocketException, IOException {
        clientSocket.setSoTimeout(timeoutMillis);
        return receiveMatchingPacket();
    }

    private byte[] receiveMatchingPacket() throws SocketException, IOException {
        byte[] payload = null;
        while (payload == null) {
            DatagramPacket receivePacket = receivePacket();
            payload = detachHeader(receivePacket.getData());
        }
        return payload;
    }

    private byte[] detachHeader(byte[] data) throws IOException {
        ByteArrayInputStream rawData = new ByteArrayInputStream(data);
        try {
            DataInput input = new DataInputStream(rawData);
            int packetNumber = input.readShort();
            if (checkPacketNumber) {
                if (packetNumber < this.packetNumber) {
                    logger.warn("Ignoring packet with smaller number #"
                            + packetNumber + " != expected packet number #"
                            + this.packetNumber);
                    return null;
                } else if (packetNumber > this.packetNumber) {
                    throw new IllegalStateException("Packet-Number mismatch");
                }
            }
            int size = input.readShort();
            byte[] content = new byte[size];
            input.readFully(content);
            return content;
        } finally {
            rawData.close();
        }
    }

    private DatagramPacket receivePacket() throws SocketException, IOException {
        byte[] receiveData = new byte[PacketBufferSize];
        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                receiveData.length);
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

    public boolean getCheckPacketNumber() {
        return checkPacketNumber;
    }

    public void setCheckPacketNumber(boolean enabled) {
        this.checkPacketNumber = enabled;
    }
}
