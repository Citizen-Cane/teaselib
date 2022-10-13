package teaselib.core.ai.perception;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import teaselib.core.ai.perception.SceneCapture.Rotation;
import teaselib.core.jni.NativeObject;

public class HumanPose extends NativeObject.Disposible {

    public HumanPose() {
        super(newNativeInstance());
    }

    private static native long newNativeInstance();

    public interface PoseAspect {
        // Tag interface
    }

    public enum Interest {
        Status(1),
        Proximity(2),
        HeadGestures(4),

        UpperTorso(8),
        LowerTorso(16),
        LegsAndFeet(32),

        MultiPose(256), //

        ;

        final int bit;

        private Interest(int bit) {
            this.bit = bit;
        }

        public static final Set<Interest> supported = asSet(Status, Proximity, HeadGestures, UpperTorso, LowerTorso, LegsAndFeet, MultiPose);
        public static final Set<Interest> Head = asSet(Status, Proximity, UpperTorso);
        public static final Set<Interest> AllPersons = asSet(Status, Proximity, MultiPose);
        public static final Set<Interest> Pose = asSet(Status, Proximity, UpperTorso, LowerTorso, LegsAndFeet);
    }

    @SafeVarargs
    public static <T> Set<T> asSet(T... interests) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(interests)));
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

        ;

        private final int distance;

        private Proximity(int distance) {
            this.distance = distance;
        }

        public static final Proximity[] NotFace2Face = { AWAY, FAR, NEAR, CLOSE };

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

            public static final Gaze None = new Gaze(0.0f, 0.0f, 0.0f);
            public final float nod;
            public final float shake;
            public final float tilt;

            public Gaze(float nod, float shake, float tilt) {
                this.nod = nod;
                this.shake = shake;
                this.tilt = tilt;
            }

            /**
             * Maximum detectable gaze values
             */
            static final class Limits {

                private Limits() { //
                }

                static final float NOD = teaselib.util.math.Unit.rad(45.0f);
                static final float SHAKE = teaselib.util.math.Unit.rad(25.0f);
            }

            public boolean isFace2Face() {
                return Math.abs(nod) < Limits.NOD && Math.abs(shake) < Limits.SHAKE;
            }

            @Override
            public String toString() {
                return "[nod=" + nod + ", shake=" + shake + ", tilt=" + tilt + "]";
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
            this(distance, new Point2D.Float(x, y), new Gaze(s, t, u));
        }

        public Estimation(float distance, Point2D.Float head) {
            this.distance = Optional.of(distance);
            this.head = Optional.of(head);
            this.gaze = Optional.empty();
        }

        public Estimation(float distance, Point2D.Float head, Gaze gaze) {
            this.distance = Optional.of(distance);
            this.head = Optional.of(head);
            this.gaze = Optional.of(gaze);
        }

        public Proximity proximity() {
            return proximity(1.0f);
        }

        public Proximity proximity(float distanceFactor) {
            if (distance.isPresent()) {
                float z = distance.get();
                Proximity proximity;
                if (z < 0.5f * distanceFactor) {
                    proximity = Proximity.CLOSE;
                } else if (z < 1.5f * distanceFactor) {
                    proximity = gaze.map(Gaze::isFace2Face).orElse(false) ? Proximity.FACE2FACE : Proximity.NEAR;
                } else if (z < 3.0f * distanceFactor) {
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

        public Optional<Rectangle2D> face() {
            if (head.isEmpty()) {
                return Optional.empty();
            } else {
                Point2D h = head.get();
                float r = 1.0f / distance.orElse(1.0f) * 0.1f;
                return Optional.of(new Rectangle2D.Double(h.getX() - r, h.getY() - r, 2 * r, 2 * r));
            }
        }

        public Optional<Rectangle2D> boobs() {
            Optional<Rectangle2D> face = face();
            if (face.isEmpty()) {
                return Optional.empty();
            } else {
                var r = face.get();
                return Optional.of(new Rectangle2D.Double(r.getMinX(), r.getMinY() + r.getHeight() * 1.5, r.getWidth(),
                        r.getHeight() / 2));
            }
        }

        @Override
        public String toString() {
            var pose = new StringBuilder();
            pose.append('[');
            List<String> elements = new ArrayList<>();
            head.ifPresent(point -> elements.add("head=" + point));
            distance.ifPresent(m -> elements.add("distance=" + m + "m"));
            gaze.ifPresent(point -> elements.add("gaze=" + point));
            pose.append(elements.stream().collect(Collectors.joining(", ")));
            pose.append(']');
            return pose.toString();
        }

    }

    public void setInterests(Set<Interest> interests) {
        setInterests(interests.stream().map(a -> a.bit).reduce(0, (a, b) -> a | b));
    }

    // TODO Parameter of estimate() or handle multiple models in Java
    // - handle in Java for simpler warm-up
    // - handle in AIfx to choose the right model for portrait and landscape image bytes
    // -> cache models in native code but select in Java
    private native void setInterests(int aspects);

    private native void setRotation(int rotation);

    private native boolean acquire(SceneCapture device);

    private native boolean acquireImage(byte[] bytes);

    private native List<HumanPose.Estimation> estimate(long timestamp);

    public List<HumanPose.Estimation> poses(SceneCapture device, long timestamp) {
        return poses(device, device.rotation().reverse(), timestamp);
    }

    private List<HumanPose.Estimation> poses(SceneCapture device, Rotation rotation, long timestamp) {
        if (acquire(device)) {
            setRotation(rotation.value);
            return estimate(timestamp);
        } else {
            throw new SceneCapture.DeviceLost("Camera not started or closed");
        }
    }

    public List<HumanPose.Estimation> poses(InputStream image) throws IOException {
        return poses(image, Rotation.None);
    }

    public List<HumanPose.Estimation> poses(InputStream image, Rotation rotation) throws IOException {
        try {
            return poses(image.readAllBytes(), rotation);
        } finally {
            image.close();
        }
    }

    public List<HumanPose.Estimation> poses(byte[] bytes) {
        return poses(bytes, Rotation.None);
    }

    public List<HumanPose.Estimation> poses(byte[] bytes, Rotation rotation) {
        if (acquireImage(bytes)) {
            // TODO Decode image bytes and derive rotation & model from image dimension
            setRotation(rotation.value);
            return estimate(0);
        } else {
            throw new SceneCapture.DeviceLost("Camera not started or closed");
        }
    }

    @Override
    protected native void dispose();

}
