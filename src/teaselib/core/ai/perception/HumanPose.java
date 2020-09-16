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

    private long timestamp = 0;

    public HumanPose() {
        super(init());
    }

    private static native long init();

    public interface PoseAspect {
        // Tag interface
    }

    public enum Interest {
        Status(1),
        Proximity(2),
        HeadGestures(4),

        ;

        final int bit;

        private Interest(int bit) {
            this.bit = bit;
        }
    }

    public enum Status implements PoseAspect {
        None,
        Available,
        Stream,
    }

    public enum Proximity implements PoseAspect {
        AWAY(Integer.MAX_VALUE),
        FAR(3),
        NEAR(2),
        FACE2FACE(1),
        CLOSE(0),

        NOTAWAY(5),
        NOTFAR(4),
        NOTNEAR(3),
        NOTFACE2FACE(2),
        NOTCLOSE(1),

        ;

        private final int distance;

        private Proximity(int distance) {
            this.distance = distance;
        }

        public static final Proximity[] Away = { AWAY, NOTFAR, NOTNEAR, NOTFACE2FACE, NOTCLOSE };
        public static final Proximity[] Far = { NOTAWAY, FAR, NOTNEAR, NOTFACE2FACE, NOTCLOSE };
        public static final Proximity[] Near = { NOTAWAY, NOTFAR, NEAR, NOTFACE2FACE, NOTCLOSE };
        public static final Proximity[] Face2Face = { NOTAWAY, NOTFAR, NOTNEAR, FACE2FACE, NOTCLOSE };
        public static final Proximity[] Close = { NOTAWAY, NOTFAR, NOTNEAR, NOTFACE2FACE, CLOSE };

        boolean isCloserThan(Proximity proximity) {
            return distance < proximity.distance;
        }
    }

    public enum HeadGestures implements PoseAspect {
        Gaze,
        None,
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

        public static final Estimation NONE = new HumanPose.Estimation();

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

        public Proximity proximity() {
            return proximityWithFactor(1.0f);
        }

        public Proximity proximityWithFactor(float factor) {
            if (distance.isPresent()) {
                float z = distance.get();
                Proximity proximity;
                if (z < 0.4f * factor) {
                    proximity = Proximity.CLOSE;
                } else if (z < 0.9f * factor) {
                    if (isFace2Face()) {
                        proximity = Proximity.FACE2FACE;
                    } else {
                        proximity = Proximity.NEAR;
                    }
                } else if (z < 2.0f * factor) {
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

    public void setInterests(Set<Interest> interests) {
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
        timestamp = System.currentTimeMillis();
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
        timestamp = System.currentTimeMillis();
        if (acquireImage(bytes)) {
            estimate();
            return results();
        } else {
            throw new SceneCapture.DeviceLost("Camera not started or closed");
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

}
