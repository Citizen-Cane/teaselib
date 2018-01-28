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

import teaselib.core.javacv.util.Geom;
import teaselib.core.util.TimeLine;
import teaselib.core.util.TimeLine.Slice;
import teaselib.motiondetection.Gesture;

public class HeadGestureTracker {
    private static final Logger logger = LoggerFactory.getLogger(HeadGestureTracker.class);

    static final long GesturePauseMillis = 1000;

    static final int NumberOfDirections = 4;
    static final long GestureMaxDuration = 250 * NumberOfDirections;
    static final long GestureMinDuration = 500;

    private final TrackFeatures tracker = new TrackFeatures();
    private final TimeLine<Direction> directionTimeLine = new TimeLine<>();
    private final Mat startKeyPoints = new Mat();

    public final Scalar color;

    private boolean resetTrackFeatures = true;

    public static class Parameters {
        public boolean cameraShake;
        public boolean motionDetected;
        public Rect gestureRegion;
    }

    public HeadGestureTracker(Scalar color) {
        this.color = color;
    }

    public void update(Mat videoImage, boolean motionDetected, Rect rect, long timeStamp) {
        // TODO Tail time millis is updated each frame compare to last entry
        if (!resetTrackFeatures && !motionDetected && timeStamp > directionTimeLine.tailTimeMillis()
                - directionTimeLine.tailTimeSpan() + GesturePauseMillis) {
            directionTimeLine.clear();
            tracker.clear();
            resetTrackFeatures = true;
        } else if (motionDetected && resetTrackFeatures && rect != null) {
            tracker.start(videoImage, rect);
            tracker.keyPoints().copyTo(startKeyPoints);
            resetTrackFeatures = false;
        } else if (!resetTrackFeatures) {
            tracker.update(videoImage);

            Direction direction = direction();
            if (direction != Direction.None) {
                directionTimeLine.add(direction, timeStamp);
            }
        }
    }

    private Direction direction() {
        if (tracker.previousKeyPoints().empty()) {
            return Direction.None;
        }

        FloatIndexer from = tracker.previousKeyPoints().createIndexer();
        FloatIndexer to = tracker.keyPoints().createIndexer();

        Map<Direction, Float> directions = new EnumMap<>(Direction.class);

        long n = Math.min(from.rows(), to.rows());
        for (int i = 0; i < n; i++) {
            Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));

            addDirection(directions, direction(p1, p2), Geom.distance2(p1, p2));

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

    static void addDirection(Map<Direction, Float> directions, Direction d) {
        addDirection(directions, d, 1);
    }

    static void addDirection(Map<Direction, Float> directions, Direction d, float amount) {
        if (d != Direction.None) {
            Float current = directions.get(d);
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

    static Direction direction(Map<Direction, Float> all) {
        if (logger.isDebugEnabled()) {
            logger.debug(all.toString());
        }

        float max = 0;
        Direction direction = Direction.None;
        for (Entry<Direction, Float> entry : all.entrySet()) {
            if (entry.getValue() > max) {
                direction = entry.getKey();
                max = entry.getValue();
            }
        }

        if (max > 10) {
            return direction;
        } else {
            return Direction.None;
        }
    }

    public Gesture getGesture() {
        Gesture gesture = getGesture(directionTimeLine);
        if (logger.isDebugEnabled()) {
            logger.debug(directionTimeLine.getTimeSpan(1.0).toString() + " ->" + gesture);
        }

        if (gesture == Gesture.None && directionTimeLine.tailTimeSpan() > GesturePauseMillis) {
            findNewFeaturesToTrack();
        }
        return gesture;
    }

    private void findNewFeaturesToTrack() {
        directionTimeLine.add(Direction.None, System.currentTimeMillis());
        restart();
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
        tracker.render(output, color);
    }
}
