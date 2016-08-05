package teaselib.core.jni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NativeObject {
    private static final Logger logger = LoggerFactory
            .getLogger(NativeObject.class);

    private final long nativeObject;

    protected NativeObject(long nativeObject) {
        this.nativeObject = nativeObject;
    }

    protected native void disposeNativeObject();

    @Override
    protected void finalize() throws Throwable {
        try {
            disposeNativeObject();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        super.finalize();
    }

}
