package teaselib.core.devices;

public interface Device {

    /**
     * Tag interface to indicate the class implements getDeviceCache(...).
     * 
     * @author Citizen-Cane
     *
     */
    public interface Creatable extends Device {
        // Tagging interface
    }

    /**
     * A placeholder name for a device that is going to be connected later on
     */
    public static final String WaitingForConnection = "WaitingForConnection";

    /**
     * Get the device path of the device. This can be used to persist a setting and create the device again.
     * 
     * @return
     */
    String getDevicePath();

    /**
     * The name of the device.
     * 
     * @return A human-readable device name that identifies the device.
     */
    String getName();

    /**
     * Test whether the device is connected and accessible.
     * <p>
     * Calling this method on a disconnected device may cause it to look for its resource, so don't call this too often.
     * To conveniently connect a device to its resource, see {@link RemoteDevices#connect}.
     */
    boolean connected();

    /**
     * Test whether the device is active, e.g it is connected, and will respond to device specific commands.
     */
    boolean active();

    /**
     * Release the device and make it available to other applications
     */
    void close();

    boolean isWireless();

    BatteryLevel batteryLevel();
}
