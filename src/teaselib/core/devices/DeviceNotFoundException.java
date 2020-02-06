package teaselib.core.devices;

public final class DeviceNotFoundException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public DeviceNotFoundException(String message) {
        super(message);
    }

    public DeviceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}