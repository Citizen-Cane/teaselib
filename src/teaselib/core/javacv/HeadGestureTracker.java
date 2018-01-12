package teaselib.core.javacv;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.util.TimeLine;
import teaselib.motiondetection.Gesture;

public class HeadGestureTracker {
    private static final Logger logger = LoggerFactory.getLogger(HeadGestureTracker.class);

    private static final long GesturePauseMillis = 500;

    private final TrackFeatures tracker = new TrackFeatures();
    private final TimeLine<Direction> directionTimeLine = new TimeLine<>();

    public final Scalar color;

    private boolean resetTrackFeatures = false;

    public HeadGestureTracker(Scalar color) {
        this.color = color;
    }

    public void update(Mat videoImage, boolean motionDetected, long timeStamp) {
        if (!motionDetected && resetTrackFeatures) {
            tracker.start(videoImage, null);
            directionTimeLine.clear();
            resetTrackFeatures = false;
        } else if (!motionDetected && directionTimeLine.tailTimeMillis() + GesturePauseMillis > timeStamp) {
            resetTrackFeatures = true;
        } else if (motionDetected && !resetTrackFeatures) {
            tracker.update(videoImage);
            directionTimeLine.add(direction(), timeStamp);
        }
    }

    private Direction direction() {
        FloatIndexer from = tracker.previousKeyPoints().createIndexer();
        FloatIndexer to = tracker.keyPoints().createIndexer();

        Map<Direction, Integer> all = new EnumMap<>(Direction.class);

        StringBuilder points = new StringBuilder();

        long n = Math.min(from.rows(), to.rows());
        for (int i = 0; i < n; i++) {
            Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
            Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));

            points.append(toString(p1) + "-" + toString(p2) + " ");

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

        from.release();
        to.release();

        if (!all.isEmpty()) {
            logger.info(points.toString());
        }

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
        logger.info(all.toString());
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
        if (direction == Direction.Right || direction == Direction.Left) {
            return Gesture.Shake;
        } else if (direction == Direction.Up || direction == Direction.Down) {
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
        // Mat currentKeyPoints = tracker.keyPoints();
        //
        // FloatIndexer from = keyPoints.createIndexer();
        // FloatIndexer to = currentKeyPoints.createIndexer();
        // long n = Math.min(from.rows(), to.rows());
        // for (int i = 0; i < n; i++) {
        // Point p1 = new Point((int) from.get(i, 0), (int) from.get(i, 1));
        // Point p2 = new Point((int) to.get(i, 0), (int) to.get(i, 1));
        // opencv_imgproc.line(output, p1, p2, color);
        // p1.close();
        // p2.close();
        // }
        //
        // from.release();
        // to.release();

        tracker.render(output, color);
    }
}
