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

    private static final long GesturePauseMillis = 1000;

    private final TrackFeatures tracker = new TrackFeatures();
    private final TimeLine<Direction> directionTimeLine = new TimeLine<>();
    private final Mat startKeyPoints = new Mat();

    public final Scalar color;

    private boolean resetTrackFeatures = false;

    public HeadGestureTracker(Scalar color) {
        this.color = color;
    }

    public void update(Mat videoImage, boolean motionDetected, Rect rect, long timeStamp) {
        if (!motionDetected && timeStamp > directionTimeLine.tailTimeMillis() + GesturePauseMillis) {
            resetTrackFeatures = true;
        } else if (motionDetected && resetTrackFeatures) {
            // TODO At startup, motion region is the whole image -> fix
            tracker.start(videoImage, rect);
            tracker.keyPoints().copyTo(startKeyPoints);
            directionTimeLine.clear();
            resetTrackFeatures = false;
        } else if (motionDetected && !resetTrackFeatures) {
            tracker.update(videoImage);
            directionTimeLine.add(direction(), timeStamp);
        }
    }

    private Direction direction() {
        FloatIndexer from = startKeyPoints.createIndexer();
        FloatIndexer to = tracker.keyPoints().createIndexer();

        Map<Direction, Integer> all = new EnumMap<>(Direction.class);

        long n = Math.min(from.rows(), to.rows());
        for (int i = 0; i < n; i++) {
            Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));

            Direction d = direction(p1, p2);
            if (d != Direction.None) {
                Integer current = all.get(d);
                if (current == null) {
                    all.put(d, 1);
                } else
                    all.put(d, current + 1);
            }

            p1.close();
            p2.close();
        }

        if (!all.isEmpty() && logger.isDebugEnabled()) {
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

        return all.isEmpty() ? Direction.None : direction(all);
    }

    private static String toString(Point p) {
        return p.x() + "x" + p.y();
    }

    private Direction direction(Point p1, Point p2) {
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

    private Direction direction(Map<Direction, Integer> all) {
        if (logger.isDebugEnabled()) {
            logger.debug(all.toString());
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
        if (directionTimeLine.size() == 0)
            return Gesture.None;
        Direction direction = directionTimeLine.tail();
        if (direction == Direction.None)
            return Gesture.None;

        int numberOfActions = 2;
        long gestureMaxDuration = 5000;
        long gestureMinDuration = 500;

        List<Slice<Direction>> slices = directionTimeLine.lastSlices(2);
        long duration = directionTimeLine.duration(slices);
        if (slices.size() == numberOfActions && gestureMinDuration < duration && duration < gestureMaxDuration) {
            long h = slices.stream().filter((slice) -> horizontal(slice.item)).count();
            long v = slices.stream().filter((slice) -> vertical(slice.item)).count();
            if (h >= numberOfActions * 2 && v == 0) {
                return Gesture.Shake;
            } else if (h == 0 && v >= numberOfActions * 2) {
                return Gesture.Nod;
            } else {
                return Gesture.None;
            }
        } else {
            return Gesture.None;
        }
    }

    private boolean vertical(Direction direction) {
        return direction == Direction.Up || direction == Direction.Down;
    }

    private boolean horizontal(Direction direction) {
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
        return tracker.haveFeatures();
    }

    public int size(Mat points) {
        return points.rows();
    }

    public void render(Mat output) {
        tracker.render(output, color);
    }
}
