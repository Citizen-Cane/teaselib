package teaselib.core.jni;

/**
 * @author Citizen-Cane
 *
 */
public class COMException extends NativeException {
    private static final long serialVersionUID = 1L;

    public COMException(int errorCode, String message) {
        super(errorCode, message);
    }

}
