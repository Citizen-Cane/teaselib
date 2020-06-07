package teaselib.core.ai.perception;

import java.util.Set;

import teaselib.core.jni.NativeObject;

public class HumanPose extends NativeObject {
    public final SceneCapture device;

    public HumanPose(SceneCapture device) {
        super(init(device));
        this.device = device;
    }

    private static native long init(SceneCapture device);

    public enum Aspect {
        None(0),
        Awareness(1),
        Head(2)

        ;

        final int bit;

        private Aspect(int bit) {
            this.bit = bit;
        }
    }

    public void setDesiredAspects(Set<Aspect> aspects) {
        setDesiredAspects(aspects.stream().map(a -> a.bit).reduce(0, (a, b) -> a | b));
    }

    private native void setDesiredAspects(int aspects);

    private native int estimatePose();

    public int estimate() {
        if (!device.isStarted()) {
            device.start();
        }

        int humans = estimatePose();
        return humans;
    }

    @Override
    public void close() {
        if (device.isStarted()) {
            device.stop();
        }
        super.close();
    }

}
