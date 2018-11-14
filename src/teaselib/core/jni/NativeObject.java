package teaselib.core.jni;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NativeObject implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(NativeObject.class);

    private final long nativeObject;

    protected NativeObject(long nativeObject) {
        this.nativeObject = nativeObject;
    }

    protected native void disposeNativeObject();

    @Override
    public void close() {
        disposeNativeObject();
    }
}
