package teaselib.core.jni;

import java.io.Closeable;

public abstract class NativeObject implements Closeable {

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
