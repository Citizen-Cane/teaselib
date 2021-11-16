package teaselib.core.ai;

import teaselib.core.jni.NativeObject;

public class ComputeContext extends NativeObject.Disposible {

    public static final ComputeContext None = new ComputeContext(0, false);

    public final boolean accelerated;

    private ComputeContext(long nativeObject, boolean accelerated) {
        super(nativeObject);
        this.accelerated = accelerated;
    }

    public static native ComputeContext newInstance();

    @Override
    protected native void dispose();

}
