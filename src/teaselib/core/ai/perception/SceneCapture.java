package teaselib.core.ai.perception;

import teaselib.core.jni.NativeObject;

public class SceneCapture extends NativeObject {
    final String name;

    public SceneCapture(long nativeObject, String name, int id) {
        super(nativeObject);
        this.name = name;
        init(id);
    }

    public SceneCapture(long nativeObject, String name, String path) {
        super(nativeObject);
        this.name = name;
    }

    native void init(int id);

    native void init(String path);

    native void start();

    native boolean isStarted();

    native void stop();
}
