package teaselib.motiondetection;

import java.util.LinkedList;

public class DirectionHistory {

    private final int frames;

    private final LinkedList<Integer> values = new LinkedList<Integer>();

    public enum Direction {
        None,
        Positive,
        Negative;
    }

    DirectionHistory(int frames) {
        this.frames = frames;
    }

    public void clear() {
        values.clear();
    }

    public void add(int value) {
        values.addLast(value);
        if (values.size() > frames) {
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
            if (values.size() > frames) {
                values.removeFirst();
            }
        }
    }

    public Direction direction(int pastFrames, int threshold) {
        if (values.isEmpty()) {
            return Direction.None;
        } else {
            int distance = distance(pastFrames);
            if (distance > threshold) {
                return Direction.Positive;
            } else if (distance < -threshold) {
                return Direction.Negative;
            } else {
                return Direction.None;
            }
        }
    }

    public int distance(int pastFrames) {
        final int first;
        int size = values.size();
        if (size == 0) {
            return 0;
        } else {
            if (size < pastFrames) {
                first = values.getFirst();
            } else {
                first = values.get(size - pastFrames);
            }
            int last = values.getLast();
            int distance = last - first;
            return distance;
        }
    }
}
