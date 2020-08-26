package teaselib.core.ai.perception;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import teaselib.core.ai.perception.SceneCapture.Rotation;
import teaselib.core.jni.NativeObject;

public class HumanPose extends NativeObject {

    public HumanPose() {
        super(init());
    }

    private static native long init();

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
        AWAY,
        FAR,
        NEAR,
        FACE2FACE,
        CLOSE,

        NOTAWAY,
        NOTFAR,
        NOTNEAR,
        NOTFACE2FACE,
        NOTCLOSE,

        ;

        public static final Proximity[] Away = { AWAY, NOTFAR, NOTNEAR, NOTFACE2FACE, NOTCLOSE };
        public static final Proximity[] Far = { NOTAWAY, FAR, NOTNEAR, NOTFACE2FACE, NOTCLOSE };
        public static final Proximity[] Near = { NOTAWAY, NOTFAR, NEAR, NOTFACE2FACE, NOTCLOSE };
        public static final Proximity[] Face2Face = { NOTAWAY, NOTFAR, NOTNEAR, FACE2FACE, NOTCLOSE };
        public static final Proximity[] Close = { NOTAWAY, NOTFAR, NOTNEAR, NOTFACE2FACE, CLOSE };
    }

    public static class EstimationResult {

        public static class Gaze {
            final float nod;
            final float shake;
            final float tilt;

            public Gaze(float nod, float shake, float tilt) {
                this.nod = nod;
                this.shake = shake;
                this.tilt = tilt;
            }

        }

        public final float distance;
        public final Point2D head;
        public final Gaze gaze;

        public EstimationResult(float distance, float x, float y, float s, float t, float u) {
            this.distance = distance;
            this.head = new Point2D.Float(x, y);
            this.gaze = new Gaze(s, t, u);
        }

        public Proximity proximity() {
            Proximity proximity;
            if (distance < 0.4f) {
                proximity = Proximity.CLOSE;
            } else if (distance < 0.9f) {
                if (isFace2Face()) {
                    proximity = Proximity.FACE2FACE;
                } else {
                    proximity = Proximity.NEAR;
                }
            } else if (distance < 2.0f) {
                proximity = Proximity.NEAR;
            } else {
                proximity = Proximity.FAR;
            }
            return proximity;
        }

        static final class Limits {

            private Limits() { //
            }

            static final float NOD = teaselib.util.math.Unit.rad(60.0f);
            static final float SHAKE = teaselib.util.math.Unit.rad(25.0f);
        }

        private boolean isFace2Face() {
            return Math.abs(gaze.nod) < Limits.NOD && Math.abs(gaze.shake) < Limits.SHAKE;
        }

    }

    public void setInterests(Set<Interests> interests) {
        setInterests(interests.stream().map(a -> a.bit).reduce(0, (a, b) -> a | b));
    }

    private native void setInterests(int aspects);

    private native boolean acquire(SceneCapture device, Rotation rotation);

    private native boolean acquireImage(byte[] bytes);

    private native void estimate();

    private native List<HumanPose.EstimationResult> results();

    public List<HumanPose.EstimationResult> poses(SceneCapture device) {
        return poses(device, device.rotation());
    }

    public List<HumanPose.EstimationResult> poses(SceneCapture device, Rotation rotation) {
        // TODO test Rotation.None in C++ code instead of setting it to null here
        if (acquire(device, rotation == Rotation.None ? null : rotation)) {
            estimate();
            return results();
        } else {
            throw new SceneCapture.DeviceLost("Camera not started or closed");
        }
    }

    public List<HumanPose.EstimationResult> poses(InputStream image) throws IOException {
        byte[] bytes = image.readAllBytes();
        if (acquireImage(bytes)) {
            estimate();
            return results();
        } else {
            throw new SceneCapture.DeviceLost("Camera not started or closed");
        }
    }

}
