package teaselib.core.ai.perception;

import java.util.Set;

import teaselib.core.ai.perception.SceneCapture.Rotation;
import teaselib.core.jni.NativeObject;

public class HumanPose extends NativeObject {
    public final SceneCapture device;

    public HumanPose(SceneCapture device) {
        // TODO get device orientation and replace prototype code
        this(device, null /* SceneCapture.Rotation.None */);
    }

    public HumanPose(SceneCapture device, Rotation rotation) {
        super(init(device, rotation));
        this.device = device;
    }

    private static native long init(SceneCapture device, Rotation rotation);

    public interface PoseAspect {
        // Tag interface
    }

    public enum Interests {
        Status(1),
        Proximity(2),

        ;

        final int bit;

        private Interests(int bit) {
            this.bit = bit;
        }
    }

    public enum Status implements PoseAspect {
        None,
        Available,
    }

    public enum Proximity implements PoseAspect {
        Away,
        Far,
        Near,
        FaceToFace,
        Close,

        NotAway,
        NotFar,
        NotNear,
        NotFaceToFace,
        NotClose
    }

    public static final Proximity Face3Face[] = { Proximity.NotAway, Proximity.NotFar, Proximity.FaceToFace,
            Proximity.NotClose };

    public void setInterests(Set<Interests> interests) {
        setInterests(interests.stream().map(a -> a.bit).reduce(0, (a, b) -> a | b));
    }

    private native void setInterests(int aspects);

    private native int estimatePose();

    public int estimate() {
        if (!device.isStarted()) {
            device.start();
        }

        return estimatePose();
    }

    @Override
    public void close() {
        if (device.isStarted()) {
            device.stop();
        }
        super.close();
    }

}
