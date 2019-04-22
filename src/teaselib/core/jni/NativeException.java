package teaselib.core.jni;

/**
 * @author Citizen-Cane
 *
 */
public class NativeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int errorCode;

    public NativeException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

}
