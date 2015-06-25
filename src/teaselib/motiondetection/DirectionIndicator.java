package teaselib.motiondetection;

import java.util.LinkedList;

public class DirectionIndicator {

    private static final int Frames = 400;

    private final LinkedList<Integer> values = new LinkedList<Integer>();

    public enum Direction {
        None, Positive, Negative;
    }

    DirectionIndicator() {
    }

    public void clear() {
        values.clear();
    }

    public void add(int value) {
        values.addLast(value);
        if (values.size() > Frames) {
            values.removeFirst();
        }
    }

    /**
     * Add the last value again to continue the stream of motion coordinates
     * when motion has stopped. Otherwise the distance() and direction()
     * functions would return wrong results.
     */
    public void addLastValueAgain() {
        if (values.size() == 0) {
            return;
        } else {
            values.addLast(values.getLast());
            if (values.size() > Frames) {
                values.removeFirst();
            }
        }
    }

    public Direction direct(int frames, int threshold) {
        if (values.isEmpty()) {
            return Direction.None;
        } else {
            int distance = distance(frames);
            if (distance > threshold) {
                return Direction.Positive;
            } else if (distance < -threshold) {
                return Direction.Negative;
            } else {
                return Direction.None;
            }
        }
    }

    public int distance(int frames) {
        final int first;
        int size = values.size();
        if (size == 0) {
            return 0;
        } else {
            if (size < frames) {
                first = values.getFirst();
            } else {
                first = values.get(size - frames);
            }
            int last = values.getLast();
            int distance = last - first;
            return distance;
        }
    }
}
