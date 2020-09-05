package teaselib.core.ai.perception;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
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

    public static class Estimation {

        public static class Gaze {

            public final float nod;
            public final float shake;
            public final float tilt;

            public Gaze(float x, float y, float nod, float shake, float tilt) {
                this.nod = nod;
                this.shake = shake;
                this.tilt = tilt;
            }

        }

        public final Optional<Float> distance;
        public final Optional<Point2D> head;
        public final Optional<Gaze> gaze;

        public Estimation() {
            this.distance = Optional.empty();
            this.head = Optional.empty();
            this.gaze = Optional.empty();
        }

        public Estimation(float distance) {
            this.distance = Optional.of(distance);
            this.head = Optional.empty();
            this.gaze = Optional.empty();
        }

        public Estimation(float distance, float x, float y) {
            this.distance = Optional.of(distance);
            this.head = Optional.of(new Point2D.Float(x, y));
            this.gaze = Optional.empty();
        }

        public Estimation(float distance, float x, float y, float s, float t, float u) {
            this.distance = Optional.of(distance);
            this.head = Optional.of(new Point2D.Float(x, y));
            this.gaze = Optional.of(new Gaze(x, y, s, t, u));
        }

        // TDOO implement latency-free proximity hysteresis (based on previous distance value)
        // -> frees Reaction listeners from implementing a sub-optimal solution
        //
        // TODO only change to a neighboring proximity value - not from Close to Away or so
        // - keep value or move in the direction - much like hysteresis
        //
        // TODO Observe image corners to determine whether the slave torso is close to the camera
        // - only if a near-ground-truth-solution is possible
        public Proximity proximity() {
            if (distance.isPresent()) {
                float z = distance.get();
                Proximity proximity;
                if (z < 0.5f) {
                    proximity = Proximity.CLOSE;
                } else if (z < 0.9f) {
                    if (isFace2Face()) {
                        proximity = Proximity.FACE2FACE;
                    } else {
                        proximity = Proximity.NEAR;
                    }
                } else if (z < 2.0f) {
                    proximity = Proximity.NEAR;
                } else {
                    proximity = Proximity.FAR;
                }
                return proximity;
            } else {
                // might be far or near, but always only partial posture
                // TODO estimate missing distance from previous value and direction,
                // and return the corresponding proximity value
                return Proximity.FAR;
                // Never returns Proximity.Away,
                // since in this case there wouldn't be any estimation result at all
            }
        }

        static final class Limits {

            private Limits() { //
            }

            static final float NOD = teaselib.util.math.Unit.rad(60.0f);
            static final float SHAKE = teaselib.util.math.Unit.rad(25.0f);
        }

        private boolean isFace2Face() {
            if (gaze.isPresent()) {
                return Math.abs(gaze.get().nod) < Limits.NOD && Math.abs(gaze.get().shake) < Limits.SHAKE;
            } else {
                return false;
            }
        }

    }

    public void setInterests(Set<Interests> interests) {
        setInterests(interests.stream().map(a -> a.bit).reduce(0, (a, b) -> a | b));
    }

    private native void setInterests(int aspects);

    private native boolean acquire(SceneCapture device, Rotation rotation);

    private native boolean acquireImage(byte[] bytes);

    private native void estimate();

    private native List<HumanPose.Estimation> results();

    public List<HumanPose.Estimation> poses(SceneCapture device) {
        return poses(device, device.rotation());
    }

    public List<HumanPose.Estimation> poses(SceneCapture device, Rotation rotation) {
        // TODO test Rotation.None in C++ code instead of setting it to null here
        if (acquire(device, rotation == Rotation.None ? null : rotation)) {
            estimate();
            return results();
        } else {
            throw new SceneCapture.DeviceLost("Camera not started or closed");
        }
    }

    public List<HumanPose.Estimation> poses(InputStream image) throws IOException {
        try {
            return poses(image.readAllBytes());
        } finally {
            image.close();
        }
    }

    public List<HumanPose.Estimation> poses(byte[] bytes) {
        if (acquireImage(bytes)) {
            estimate();
            return results();
        } else {
            throw new SceneCapture.DeviceLost("Camera not started or closed");
        }
    }

}
