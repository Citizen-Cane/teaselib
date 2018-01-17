package teaselib.core.javacv;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.TimeLine;
import teaselib.core.util.TimeLine.Slice;
import teaselib.motiondetection.Gesture;

public class HeadGestureTracker {
    private static final Logger logger = LoggerFactory.getLogger(HeadGestureTracker.class);

    static final long GesturePauseMillis = 1000;

    static final int NumberOfDirections = 4;
    static final long GestureMaxDuration = 5000;
    static final long GestureMinDuration = 500;

    private final TrackFeatures tracker = new TrackFeatures();
    private final TimeLine<Direction> directionTimeLine = new TimeLine<>();
    private final Mat startKeyPoints = new Mat();

    public final Scalar color;

    private boolean resetTrackFeatures = false;

    public HeadGestureTracker(Scalar color) {
        this.color = color;
    }

    public void update(Mat videoImage, boolean motionDetected, Rect rect, long timeStamp) {
        // TODO Tail time millis is updated each frame compare to last entry
        if (!resetTrackFeatures && !motionDetected && timeStamp > directionTimeLine.tailTimeMillis()
                - directionTimeLine.tailTimeSpan() + GesturePauseMillis) {
            directionTimeLine.clear();
            // TODO clearing time line may cause exception when querying in if-clause
            resetTrackFeatures = true;
        } else if (motionDetected && resetTrackFeatures) {
            // TODO At startup, motion region is the whole image -> fix
            // TODO presence region may collapse to eyes - enlarge to motion region
            tracker.start(videoImage, rect);
            tracker.keyPoints().copyTo(startKeyPoints);
            resetTrackFeatures = false;
        } else if (!resetTrackFeatures) {
            // TODO tracked points run off
            tracker.update(videoImage);
            directionTimeLine.add(direction(), timeStamp);
        }
    }

    private Direction direction() {
        FloatIndexer from = startKeyPoints.createIndexer();
        FloatIndexer to = tracker.keyPoints().createIndexer();

        Map<Direction, Integer> directions = new EnumMap<>(Direction.class);

        long n = Math.min(from.rows(), to.rows());
        for (int i = 0; i < n; i++) {
            Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));

            addDirection(directions, direction(p1, p2));

            p1.close();
            p2.close();
        }

        if (!directions.isEmpty() && logger.isDebugEnabled()) {
            StringBuilder points = new StringBuilder();
            for (int i = 0; i < n; i++) {
                Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
                Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));
                points.append(toString(p1) + "-" + toString(p2) + " ");
                p1.close();
                p2.close();
            }
            logger.debug(points.toString());
        }

        from.release();
        to.release();

        try {
            from.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        try {
            to.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return directions.isEmpty() ? Direction.None : direction(directions);
    }

    static void addDirection(Map<Direction, Integer> directions, Direction d) {
        addDirection(directions, d, 1);
    }

    static void addDirection(Map<Direction, Integer> directions, Direction d, int amount) {
        if (d != Direction.None) {
            Integer current = directions.get(d);
            if (current == null) {
                directions.put(d, amount);
            } else
                directions.put(d, current + amount);
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

    static Direction direction(Map<Direction, Integer> all) {
        if (logger.isInfoEnabled()) {
            logger.info(all.toString());
        }

        int max = 0;
        Direction direction = Direction.None;
        for (Entry<Direction, Integer> entry : all.entrySet()) {
            if (entry.getValue() > max) {
                direction = entry.getKey();
                max = entry.getValue();
            }
        }
        return direction;
    }

    public Gesture getGesture() {
        return getGesture(directionTimeLine);
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
            long h = slices.stream().filter(slice -> horizontal(slice.item)).count();
            long v = slices.stream().filter(slice -> vertical(slice.item)).count();
            if (h > v && h >= NumberOfDirections && v <= 1) {
                return Gesture.Shake;
            } else if (v > h && v >= NumberOfDirections && h <= 1) {
                return Gesture.Nod;
            } else {
                return Gesture.None;
            }
        } else {
            return Gesture.None;
        }
    }

    private static boolean vertical(Direction direction) {
        return direction == Direction.Up || direction == Direction.Down;
    }

    private static boolean horizontal(Direction direction) {
        return direction == Direction.Right || direction == Direction.Left;
    }

    public Gesture getGestureSimple() {
        if (directionTimeLine.size() == 0)
            return Gesture.None;
        Direction direction = directionTimeLine.tail();
        if (direction == Direction.None)
            return Gesture.None;
        if (horizontal(direction)) {
            return Gesture.Shake;
        } else if (vertical(direction)) {
            return Gesture.Nod;
        } else {
            throw new IllegalStateException(directionTimeLine.toString());
        }
    }

    public void reset() {
        resetTrackFeatures = true;
    }

    public boolean hasFeatures() {
        return tracker.hasFeatures();
    }

    public int size(Mat points) {
        return points.rows();
    }

    public void render(Mat output) {
        tracker.render(output, color);
    }
}
