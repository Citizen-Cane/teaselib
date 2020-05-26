package teaselib.core.ai.perception;

import teaselib.core.jni.NativeObject;

public class SceneCapture extends NativeObject {
    final String name;

    SceneCapture(long nativeObject, String name) {
        super(nativeObject);
        this.name = name;
    }

    public SceneCapture(String name, String path) {
        super(init(path));
        this.name = name;
    }

    private static native long init(String path);

    native void start();

    native boolean isStarted();

    native void stop();
}
