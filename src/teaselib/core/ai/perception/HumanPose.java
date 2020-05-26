package teaselib.core.ai.perception;

import static java.util.Arrays.stream;

import teaselib.core.events.EventSource;
import teaselib.core.jni.NativeObject;

public class HumanPose extends NativeObject {
    public final SceneCapture device;

    static class EventArgs extends teaselib.core.events.EventArgs {
        public final Aspect aspect;

        public EventArgs(Aspect aspect) {
            this.aspect = aspect;
        }

    }

    public EventSource<EventArgs> pose = new EventSource<>("Human Pose");

    public HumanPose(SceneCapture device) {
        super(init(device));
        this.device = device;
    }

    private static native long init(SceneCapture device);

    enum Aspect {
        None(0),
        Awareness(1),
        Head(2)

        ;

        final int bit;

        private Aspect(int bit) {
            this.bit = bit;
        }
    }

    public void setDesiredAspects(Aspect... aspects) {
        setDesiredAspects(stream(aspects).map(a -> a.bit).reduce(0, (a, b) -> a | b));
    }

    private native void setDesiredAspects(int aspects);

    private native int estimatePose();

    public int estimate() {
        if (!device.isStarted()) {
            device.start();
        }

        int humans = estimatePose();
        if (humans == 0) {
            pose.fire(new EventArgs(Aspect.None));
        } else {
            pose.fire(new EventArgs(Aspect.Awareness));
        }

        return humans;
    }

    @Override
    public void close() {
        if (device.isStarted()) {
            device.stop();
        }
        device.close();
        super.close();
    }

}
