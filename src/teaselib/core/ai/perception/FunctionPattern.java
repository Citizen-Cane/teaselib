package teaselib.core.ai.perception;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionPattern {

    private static final Logger logger = LoggerFactory.getLogger(FunctionPattern.class);

    public interface Direction {
        static final float MIN_SIGNAL_VALUE = 15.0f;
        static final int MAX_DURATION_MILLIS = 750;

        Direction None = new Direction() {

            @Override
            public String toString() {
                return "None";
            }
        };

        enum Rejected implements Direction {
            TooLong,
            TooSmall
        }

    }

    private class DirectionValue {
        final Direction direction;
        final float x;
        final float dx;

        private DirectionValue(Direction direction, float x, float dx) {
            this.direction = direction;
            this.x = x;
            this.dx = dx;
        }

        @Override
        public String toString() {
            return direction + "=" + x + "(" + dx + ")";
        }

    }

    private final DenseTimeLine<DirectionValue> values;
    private final Direction plus;
    private final Direction minus;

    private final List<List<Direction>> patterns;

    float last = 0.0f;

    @SafeVarargs
    public FunctionPattern(int capacity, long duration, TimeUnit unit, Direction plus, Direction minus,
            List<Direction>... patterns) {
        this.values = new DenseTimeLine<>(capacity, duration, unit);
        this.plus = plus;
        this.minus = minus;
        this.patterns = Arrays.asList(patterns);
    }

    public void clear(float x, long timestamp, TimeUnit unit) {
        values.clear();
        last = 0.0f;
        values.add(new DirectionValue(Direction.None, x, 0.0f), timestamp, unit);
    }

    public boolean update(long timestamp, float x) {
        if (!Float.isNaN(x)) {
            float dx = x - last;
            if (dx != 0) {
                Direction direction;
                if (dx > 0)
                    direction = plus;
                else /* if (dx < 0) */
                    direction = minus;

                DirectionValue lastDirection = values.last();
                if (direction == lastDirection.direction) {
                    values.removeLast();
                    values.add(new DirectionValue(direction, x, lastDirection.dx + dx), timestamp, MILLISECONDS);
                } else {
                    values.add(new DirectionValue(direction, x, dx), timestamp, MILLISECONDS);
                }
                last = x;

                // TODO filter insignificant values and join adjacent entries
                DenseTimeLine<DirectionValue> tail = values.last(patterns.get(0).size());
                logger.info("movement = {}", tail);
                List<Direction> gesture = tail.last(3, TimeUnit.SECONDS).stream().map(this::direction)
                        .collect(toList());
                logger.info("Gesture pattern = {}", gesture);
                if (gesture.equals(patterns.get(0)) || gesture.equals(patterns.get(1))) {
                    return true;
                }
            }
        }

        return false;
    }

    // TODO constructor parameters
    private Direction direction(DenseTimeLine.TimeStamp<DirectionValue> element) {
        DirectionValue directionValue = element.element;
        if (Math.abs(directionValue.dx) < Direction.MIN_SIGNAL_VALUE) {
            return Direction.Rejected.TooSmall;
        } else if (element.durationMillis > Direction.MAX_DURATION_MILLIS) {
            return Direction.Rejected.TooLong;
        } else {
            return directionValue.direction;
        }
    }

    public long maxDuration(TimeUnit unit) {
        return values.maxDuration(unit);
    }
}
