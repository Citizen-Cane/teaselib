package teaselib.core.ai.perception;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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

        static Integer valueOf(Set<Interest> interests) {
            return interests.stream().map(a -> a.bit).reduce(0, (a, b) -> a | b);
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

    /**
     *
     * Proximity relative to scene capture device. The {@link Proximity#CLOSE} region is embedded in the
     * {@link Proximity#FACE2FACE} region. Both {@link #CLOSE} or {@link #FACE2FACE} are embedded in the
     * {@link Proximity#NEAR} region.
     * <p>
     * {@link Proximity#FAR} and {@link Proximity#AWAY} are disjunct.
     * <p>
     * <p>
     * Hysteresis is applied to all proximity values but {@link #CLOSE}.
     * 
     * <pre>
     *         {@code  NCCCN
     *         NFACEFN 
     *        NFACEFACN
     *       NEARNEARNEA
     *      NEARNEARNEARN
     *     FARFARFARFARFAR
     *    FARFARFARARFARFAR
     *   AWAYAWAYAWYAWAYAWAY
     *  AWAYAWAYAWWAYAWAYAWAY 
     *         }
     * </pre>
     *
     * @author Citizen-Cane
     */
    public enum Proximity implements PoseAspect {
        /**
         * Far away or absent.
         */
        AWAY(Integer.MAX_VALUE),
        /**
         * Far from the NPC, not close enough for dialog.
         */
        FAR(3),
        /**
         * In range for dialog.
         */
        NEAR(2),
        /**
         * Actually looking towards the NPC, ready for a face2face dialog.
         */
        FACE2FACE(1),
        /**
         * Too close for normal dialog
         */
        CLOSE(0),

        ;

        private final int distance;

        private Proximity(int distance) {
            this.distance = distance;
        }

        public static final Proximity[] Presence = { FAR, NEAR, FACE2FACE, CLOSE };

        public static final Proximity[] Face2Face = { FACE2FACE, CLOSE };
        public static final Proximity[] Near = { NEAR, FACE2FACE, CLOSE };
        public static final Proximity[] Far = { FAR, NEAR, FACE2FACE, CLOSE };

        @SuppressWarnings("hiding")
        static final class Not {
            public static final Proximity[] Close = { AWAY, FAR, NEAR, FACE2FACE };
            public static final Proximity[] Face2Face = { AWAY, FAR, NEAR };
            public static final Proximity[] Near = { FAR, AWAY };
            public static final Proximity[] Far = { AWAY };
        }

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

        private static final Rectangle2D.Float CloseRegion = new Rectangle2D.Float(0.1f, 0.1f, 0.8f, 0.8f);
        private static final Point2D OutOfView = new Point2D.Float(-1.0f, -1.0f);

        Proximity proximity(float distanceFactor) {
            if (distance.isPresent()) {
                float z = distance.get();
                Proximity proximity;
                if (z < 0.7f && CloseRegion.contains(head.orElse(OutOfView))) {
                    proximity = isFace2Face(gaze) ? Proximity.CLOSE : Proximity.NEAR;
                } else if (z < 1.5f * distanceFactor) {
                    proximity = isFace2Face(gaze) ? Proximity.FACE2FACE : Proximity.NEAR;
                } else if (z < 3.0f * distanceFactor) {
                    proximity = Proximity.NEAR;
                } else if (z < 6.0f * distanceFactor) {
                    proximity = Proximity.FAR;
                } else {
                    proximity = Proximity.AWAY;
                }
                return proximity;
            } else {
                return null;
            }
        }

        private static Boolean isFace2Face(Optional<Gaze> gaze) {
            return gaze.map(Gaze::isFace2Face).orElse(false);
        }

        public Optional<Rectangle2D> face() {
            if (head.isEmpty()) {
                return Optional.empty();
            } else {
                Point2D h = head.get();
                float r = 1.0f / distance.orElse(1.0f) * 0.2f;
                return Optional.of(new Rectangle2D.Double(h.getX() - r, h.getY() - r, 2 * r, 2 * r));
            }
        }

        // TODO actual bounding box
        public Optional<Rectangle2D> body() {
            if (head.isEmpty()) {
                return Optional.empty();
            } else {
                Point2D p = head.get();
                float r = 1.0f / distance.orElse(1.0f) * 0.4f;
                double h = p.getY() - r;
                return Optional.of(new Rectangle2D.Double(p.getX() - r, h, 2 * r, 1.0 - h));
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

    public void loadModel(Set<Interest> interests, Rotation rotation) {
        loadModel(HumanPose.Interest.valueOf(interests), rotation.value);
    }

    private native void loadModel(int interests, int roation);

    public void setInterests(Set<Interest> interests) {
        setInterests(HumanPose.Interest.valueOf(interests));
    }

    // TODO Parameter of estimate() or handle multiple models in Java
    // - handle in Java for simpler warm-up
    // - handle in AIfx to choose the right model for portrait and landscape image bytes
    // -> cache models in native code but select in Java
    private native void setInterests(int interests);

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
            throw new SceneCapture.DeviceLost("Image acquisition failed");
        }
    }

    public List<HumanPose.Estimation> poses(byte[] bytes, Rotation rotation) {
        if (acquireImage(bytes)) {
            setRotation(rotation.value);
            return estimate(0);
        } else {
            throw new IllegalArgumentException("Unable to compute poses from image bytes");
        }
    }

    @Override
    protected native void dispose();

}
