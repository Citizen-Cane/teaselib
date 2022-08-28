package teaselib.core.ai.perception;

import java.util.List;

import teaselib.core.jni.NativeObject;

public class Person extends NativeObject.Disposible {

    HumanPose.Estimation current = HumanPose.Estimation.NONE;

    public Person() {
        super(newNativeInstance());
    }

    private static native long newNativeInstance();

    native void startTracking();

    native void update(HumanPose model, SceneCapture device, int rotation, long timestamp);

    static native void update(List<Person> persons, HumanPose model, SceneCapture device, int rotation, long timestamp);

    @Override
    protected native void dispose();

    void update(HumanPose.Estimation pose) {
        current = pose;
    }

    HumanPose.Estimation pose() {
        return current;
    }

}
