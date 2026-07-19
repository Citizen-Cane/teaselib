package teaselib.core.speechrecognition;

import teaselib.core.jni.NativeException;

/**
 * @author Citizen-Cane
 *
 */
public class UnsupportedLanguageException extends NativeException {

    public UnsupportedLanguageException(int errorCode, String message) {
        super(errorCode, message);
    }

}
