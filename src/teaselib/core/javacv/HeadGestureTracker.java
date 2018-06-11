package teaselib.core.javacv;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.javacv.util.Geom;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.TimeLine;
import teaselib.core.util.TimeLine.Slice;
import teaselib.motiondetection.Gesture;

//TODO  number of direction == 4 much more natural but tracker detects lots of false positives (maybe a timing issue)
// -> better with tighter timing, as shaking/nodding is a short gesture (about a second)
// TODO tracker not updated although no tracking points are active, and the motion region focusses on the head
// TODO check that motion detection still works because the "Assume The Position" test does not always wait, (intended?) and sometimes it blocks 
public class HeadGestureTracker {
    private static final Logger logger = LoggerFactory.getLogger(HeadGestureTracker.class);

    // TODO res / fraction must be larger than dominating direction scale or tracking buffers
    // -> find relationship and document in code
    static final int MINIMUM_DIRECTION_SIZE_FRACTION_OF_VIDEO = 40;
    static final int DOMINATING_DIRECTION_SCALE_OVER_OTHER = 5;

    static final long GesturePauseMillis = 500;

    /**
     * THe number of single moves of a gesture.
     */
    static final int NumberOfDirections = 4;
    // TODO Tests that expect 6 directions are broken -> return to 6 if unstable

    /**
     * Duration in which the gesture has to be completed.
     */
    static final long GestureMaxDuration = 250 * NumberOfDirections;

    /**
     * The minimum duration it takes to complete a gesture.
     */
    static final long GestureMinDuration = 500;

    private final TrackFeatures tracker = new TrackFeatures(5);
    private final TimeLine<Direction> directionTimeLine = new TimeLine<>();
    private final Mat startKeyPoints = new Mat();

    public final Scalar color;
    public final Scalar colorInverse;

    private boolean resetTrackFeatures = true;
    private Rect region;

    private int videoWidth;

    public static class Parameters {
        public boolean cameraShake = true;
        public boolean motionDetected = false;
        public Rect gestureRegion = null;
    }

    public HeadGestureTracker(Scalar color) {
        this.color = color;
        this.colorInverse = new Scalar(255 - color.blue(), 255 - color.green(), 255 - color.red(), 0);
    }

    public static Rect enlargePresenceRegionToFaceSize(Mat video, Rect presence) {
        // Enlarge gesture region based on the observation that when beginning the first nod the presence region
        // starts with a horizontally wide but vertically narrow area around the eyes
        // - if the region gets too large we might miss the face
        int fraction = 5;
        try (Size minSize = new Size(video.rows() / fraction, video.cols() / fraction)) {
            if (presence.width() < minSize.width() || presence.height() < minSize.height()) {
                try (Point center = Geom.center(presence);
                        Size size = new Size(Math.max(minSize.width(), presence.width()),
                                Math.max(minSize.height(), presence.height()));) {
                    presence = new Rect(center.x() - size.width() / 2, center.y() - size.height() / 2, size.width(),
                            size.height());
                }
            }
        }
        return presence;
    }

    public void update(Mat videoImage, boolean motionDetected, Rect region, long timeStamp) {
        if (region == null) {
            throw new NullPointerException("region");
        }

        if (resetTrackFeatures) {
            restartGesture(videoImage, region, timeStamp, "Resetting timeline after no-motion timeout");
        } else if (motionStopped(motionDetected, timeStamp)) {
            restartGesture(videoImage, region, timeStamp, "Resetting timeline after motion ended & timeout");
        } else {
            updateGesture(videoImage, region, timeStamp);
        }
    }

    private boolean motionStopped(boolean motionDetected, long timeStamp) {
        return !motionDetected && featuresAreOutdated(timeStamp);
    }

    private boolean featuresAreOutdated(long timeStamp) {
        return timeStamp > directionTimeLine.tailTimeMillis() + GesturePauseMillis;
    }

    private void restartGesture(Mat videoImage, Rect region, long timeStamp, String string) {
        if (logger.isDebugEnabled()) {
            logger.debug("{} @{}", string, timeStamp);
        }
        clear();
        directionTimeLine.add(Direction.None, timeStamp);
        findNewFeatures(videoImage, region);
    }

    private void updateGesture(Mat videoImage, Rect region, long timeStamp) {
        tracker.update(videoImage);
        Direction direction = direction();
        if (direction != Direction.None) {
            directionTimeLine.add(direction, timeStamp);
        } else if (featuresAreOutdated(timeStamp)) {
            logger.debug("Updating outdated features @{}", timeStamp);
            clear();
            directionTimeLine.add(Direction.None, timeStamp);
            findNewFeatures(videoImage, region);
        }
    }

    public void findNewFeatures(Mat videoImage, Rect region) {
        this.region = region;
        tracker.start(videoImage, region);
        tracker.keyPoints().copyTo(startKeyPoints);
        resetTrackFeatures = false;
    }

    private Direction direction() {
        Mat keyPoints = tracker.keyPoints();
        Mat previousKeyPoints = tracker.previousKeyPoints();

        if (previousKeyPoints.empty()) {
            return Direction.None;
        }

        // TODO Parameter object
        Map<Direction, Float> directions = new EnumMap<>(Direction.class);
        Map<Direction, Integer> weights = new EnumMap<>(Direction.class);
        calcDirectionsAndWeights(keyPoints, previousKeyPoints, directions, weights);
        logger.debug("{} = {}", directions, weights);

        return directions.isEmpty() ? Direction.None : direction(directions, weights);
    }

    private void calcDirectionsAndWeights(Mat keyPoints, Mat previousKeyPoints, Map<Direction, Float> directions,
            Map<Direction, Integer> weights) {
        try (FloatIndexer from = previousKeyPoints.createIndexer(); FloatIndexer to = keyPoints.createIndexer();) {
            long n = Math.min(from.rows(), to.rows());
            for (int i = 0; i < n; i++) {
                try (Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
                        Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));) {
                    addDirection(directions, weights, direction(p1, p2), Geom.distance2(p1, p2));
                }
            }

            if (!directions.isEmpty() && logger.isDebugEnabled()) {
                StringBuilder points = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    try (Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
                            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));) {
                        points.append(toString(p1) + "-" + toString(p2) + " ");
                    }
                }
                logger.debug(points.toString());
            }

            from.release();
            to.release();
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    static void addDirection(Map<Direction, Float> directions, Map<Direction, Integer> weights, Direction d) {
        addDirection(directions, weights, d, 1);
    }

    static void addDirection(Map<Direction, Float> directions, Map<Direction, Integer> weights, Direction d,
            float amount) {
        if (d != Direction.None) {
            Float current = directions.get(d);
            if (current == null) {
                directions.put(d, amount);
                weights.put(d, 1);
            } else {
                directions.put(d, current + amount);
                weights.put(d, weights.get(d) + 1);
            }
        }
    }

    private static String toString(Point p) {
        return p.x() + "x" + p.y();
    }

    static Direction direction(Point p1, Point p2) {
        int px = p2.x() - p1.x();
        int py = p2.y() - p1.y();

        Direction x;
        if (px > 0) {
            x = Direction.Right;
        } else if (px < 0) {
            x = Direction.Left;
        } else {
            x = Direction.None;
        }

        Direction y;
        if (py > 0) {
            y = Direction.Down;
        } else if (py < 0) {
            y = Direction.Up;
        } else {
            y = Direction.None;
        }

        if (Math.abs(px) > Math.abs(py)) {
            y = Direction.None;
        } else if (Math.abs(px) < Math.abs(py)) {
            x = Direction.None;
        } else {
            x = Direction.None;
            y = Direction.None;
        }

        return x != Direction.None ? x : y;
    }

    Direction direction(Map<Direction, Float> all, Map<Direction, Integer> weights) {
        return direction(all, weights, videoWidth);
    }

    static Direction direction(Map<Direction, Float> all, Map<Direction, Integer> weights, int videoWidth) {
        if (logger.isDebugEnabled()) {
            logger.debug(all.toString());
        }

        float max = 0;
        Direction direction = Direction.None;
        for (Entry<Direction, Float> entry : all.entrySet()) {
            float value = entry.getValue() / weights.get(entry.getKey());
            if (value > max) {
                direction = entry.getKey();
                max = value;
            }
        }

        if (max > videoWidth / MINIMUM_DIRECTION_SIZE_FRACTION_OF_VIDEO) {
            return dominatingDirectionOrNone(all, weights, direction);
        } else {
            logger.info("Direction value {} too small", max);
            return Direction.None;
        }
    }

    private static Direction dominatingDirectionOrNone(Map<Direction, Float> all, Map<Direction, Integer> weights,
            Direction direction) {
        Float maxValue = all.get(direction);
        for (Entry<Direction, Float> entry : all.entrySet()) {
            if (entry.getKey() != direction && entry.getValue() / weights.get(entry.getKey())
                    * DOMINATING_DIRECTION_SCALE_OVER_OTHER > maxValue) {
                logger.debug("Direction {} not dominating ", direction);
                direction = Direction.None;
                break;
            }
        }
        return direction;
    }

    public Gesture getGesture() {
        Gesture gesture = getGesture(directionTimeLine);
        if (logger.isDebugEnabled()) {
            logger.debug("{} -> {}", directionTimeLine.getTimeSpan(1.0), gesture);
        }
        return gesture;
    }

    static Gesture getGesture(TimeLine<Direction> directionTimeLine) {
        if (directionTimeLine.size() == 0)
            return Gesture.None;
        Direction direction = directionTimeLine.tail();
        if (direction == Direction.None)
            return Gesture.None;

        List<Slice<Direction>> slices = directionTimeLine.getTimeSpanSlices(GestureMaxDuration);
        long duration = directionTimeLine.duration(slices);
        if (slices.size() >= NumberOfDirections && GestureMinDuration <= duration && duration <= GestureMaxDuration) {
            long n = slices.stream().filter(slice -> slice.item == Direction.None).count();
            if (n == 0) {
                long h = slices.stream().filter(slice -> horizontal(slice.item)).count();
                long v = slices.stream().filter(slice -> vertical(slice.item)).count();
                if (h > v && h >= NumberOfDirections && v <= 1) {
                    logger.info("Detected {}: {}", Gesture.Shake, slices);
                    return Gesture.Shake;
                } else if (v > h && v >= NumberOfDirections && h <= 1) {
                    logger.info("Detected {}: {}", Gesture.Nod, slices);
                    return Gesture.Nod;
                } else {
                    logger.debug("Not a distinct gesture: {}", slices);
                    return Gesture.None;
                }
            } else {
                logger.info("Timeslice contain NoDirection entries: {}", slices);
                return Gesture.None;
            }
        } else {
            logger.debug("Gesture timeout: {}ms", duration);
            return Gesture.None;
        }
    }

    private static boolean vertical(Direction direction) {
        return direction == Direction.Up || direction == Direction.Down;
    }

    private static boolean horizontal(Direction direction) {
        return direction == Direction.Right || direction == Direction.Left;
    }

    public void restart() {
        resetTrackFeatures = true;
    }

    public boolean hasFeatures() {
        return tracker.hasFeatures();
    }

    public int size(Mat points) {
        return points.rows();
    }

    public void render(Mat output) {
        tracker.render(output, color, region, colorInverse);
    }

    public void clear() {
        directionTimeLine.clear();
        tracker.clear();
        restart();
    }

    public Rect getRegion() {
        return region;
    }
}
