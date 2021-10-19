package teaselib.core.ai.perception;

import java.awt.Dimension;
import java.awt.Toolkit;

import teaselib.core.jni.NativeObject;

public class SceneCapture extends NativeObject.Disposible {
    public static final int NoImage = -1;

    public final String name;
    public final EnclosureLocation location;

    public enum EnclosureLocation {
        Front,
        Rear,
        External
    }

    public enum Rotation {
        None,
        Clockwise,
        CounterClockwise,
        UpsideDown
    }

    public static class DeviceLost extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public DeviceLost(String message) {
            super(message);
        }

    }

    SceneCapture(long nativeObject, String name, EnclosureLocation location) {
        super(nativeObject);
        this.name = name;
        this.location = location;
    }

    public SceneCapture(String openCVImagePattern) {
        super(newNativeInstance(openCVImagePattern));
        this.name = openCVImagePattern;
        this.location = EnclosureLocation.External;
    }

    private static native long newNativeInstance(String openCVImagePattern);

    public native void start();

    public native boolean isStarted();

    public native void stop();

    public Rotation rotation() {
        // TODO get device orientation and replace prototype with production code
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        boolean portrait = screenSize.width < screenSize.height;
        // The native code only understands rotation != null and rotates clockwise
        return portrait ? Rotation.Clockwise : null;
    }

    @Override
    protected native void dispose();

    @Override
    public void close() {
        if (isStarted()) {
            stop();
        }
        super.close();
    }

}
