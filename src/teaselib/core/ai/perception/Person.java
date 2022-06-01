package teaselib.core.ai.perception;

import java.util.List;

import teaselib.core.jni.NativeObject;

public class Person extends NativeObject.Disposible {

    HumanPose.Estimation current = HumanPose.Estimation.NONE;

    public Person() {
        super(newNativeInstance());
    }

    private static native long newNativeInstance();

    static native void update(Person nearest, HumanPose model, SceneCapture device, int rotation, long timestamp);

    static native void update(List<Person> persons, HumanPose model, SceneCapture device, int rotation, long timestamp);

    @Override
    protected native void dispose();

    void update(HumanPose.Estimation pose) {
        current = pose;
    }

    void trackNearest(HumanPose model, SceneCapture device, long timestamp) {
        var poses = model.poses(device, timestamp);
        if (poses.isEmpty()) {
            current = HumanPose.Estimation.NONE;
        } else {
            current = poses.get(0);
        }
    }

    HumanPose.Estimation pose() {
        return current;
    }

}
