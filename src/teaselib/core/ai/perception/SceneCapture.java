package teaselib.core.ai.perception;

import teaselib.core.jni.NativeObject;

public class SceneCapture extends NativeObject {
    public static final int NoImage = -1;

    public final String name;
    public final EnclosureLocation location;

    public enum EnclosureLocation {
        Front,
        Rear,
        External
    }

    SceneCapture(long nativeObject, String name, EnclosureLocation location) {
        super(nativeObject);
        this.name = name;
        this.location = location;
    }

    public SceneCapture(String openCVImagePattern) {
        super(init(openCVImagePattern));
        this.name = openCVImagePattern;
        this.location = EnclosureLocation.External;
    }

    private static native long init(String openCVImagePattern);

    native void start();

    native boolean isStarted();

    native void stop();
}
