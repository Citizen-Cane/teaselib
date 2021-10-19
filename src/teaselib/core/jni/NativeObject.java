package teaselib.core.jni;

import java.io.Closeable;

/**
 * Used by classes that aggregate a native instance. The native object field points to the memory location of the native
 * instance.
 * 
 * @author Citizen-Cane
 *
 */
public abstract class NativeObject {

    @SuppressWarnings("unused") // used by the native code to store data, e.g. a native object instance
    private final long nativeObject;

    protected NativeObject(long nativeObject) {
        this.nativeObject = nativeObject;
    }

    protected abstract void dispose();

    public static abstract class Aggregated extends NativeObject {

        public Aggregated(long nativeObject) {
            super(nativeObject);
        }

        @Override
        protected void dispose() {
            throw new UnsupportedOperationException();
        }
    }

    public static abstract class Disposible extends NativeObject implements Closeable {

        public Disposible(long nativeObject) {
            super(nativeObject);
        }

        @Override
        public void close() {
            dispose();
        }
    }

}
