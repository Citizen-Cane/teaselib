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

    EventSource<EventArgs> pose = new EventSource<>("Human Pose");

    public HumanPose(long nativeObject, SceneCapture device) {
        super(nativeObject);
        this.device = device;
    }

    enum Aspect {
        Awareness(1),
        Head(2)

        ;

        final int bit;

        private Aspect(int bit) {
            this.bit = bit;
        }
    }

    public void setAspects(Aspect... aspects) {
        setAspects(stream(aspects).map(a -> a.bit).reduce(0, (a, b) -> a | b));
    }

    private native void setAspects(int aspects);

    public void test() {
        pose.add(e -> e.consumed = true);
    }
}
