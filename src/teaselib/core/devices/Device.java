package teaselib.core.devices;

public interface Device {

    /**
     * A placeholder name for a device that is going to be connected later on
     */
    public static final String WaitingForConnection = "WaitingForConnection";

    /**
     * Get the device path of the device. This can be used to persist a setting
     * and create the device again.
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
     */
    boolean connected();

    /**
     * Test whether the device is active.
     */
    boolean active();

    /**
     * Release the device and make it available to other applications
     */
    void release();
}
