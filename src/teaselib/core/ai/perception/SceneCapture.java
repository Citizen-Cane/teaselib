package teaselib.core.ai.perception;

import teaselib.core.jni.NativeObject;

public class SceneCapture extends NativeObject {
    public static final int NoImage = -1;

    final String name;

    SceneCapture(long nativeObject, String name) {
        super(nativeObject);
        this.name = name;
    }

    public SceneCapture(String openCVImagePattern) {
        super(init(openCVImagePattern));
        this.name = openCVImagePattern;
    }

    private static native long init(String openCVImagePattern);

    native void start();

    native boolean isStarted();

    native void stop();
}
