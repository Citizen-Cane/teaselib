package teaselib.core.javacv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;

import teaselib.core.util.TimeLine;
import teaselib.motiondetection.Gesture;

public class HeadGestureTrackerTest {
    private static final long SHAKETIME = (HeadGestureTracker.GestureMinDuration
            + HeadGestureTracker.GestureMaxDuration) / (2 * HeadGestureTracker.NumberOfDirections);
    private static final int NUMBER_OF_SUPPORTED_DIRECTIONS = 6;
    private static final int DIRECTION_SIZE_AT_640 = (int) (640
            * HeadGestureTracker.MINIMUM_DIRECTION_SIZE_FRACTION_OF_VIDEO);

    @Test
    public void testDirection() {
        try (Point p1 = new Point(0, 0); Point p2 = new Point(0, 0);) {
            assertEquals(Direction.None, HeadGestureTracker.direction(p1, p2));
        }

        try (Point p1 = new Point(0, 0); Point p2 = new Point(1, 0);) {
            assertEquals(Direction.Right, HeadGestureTracker.direction(p1, p2));
        }

        try (Point p1 = new Point(-1, 0); Point p2 = new Point(-2, 0);) {
            assertEquals(Direction.Left, HeadGestureTracker.direction(p1, p2));
        }

        try (Point p1 = new Point(-1, -1); Point p2 = new Point(-2, -2);) {
            assertEquals(Direction.None, HeadGestureTracker.direction(p1, p2));
        }

        try (Point p1 = new Point(0, 0); Point p2 = new Point(0, 1);) {
            assertEquals(Direction.Down, HeadGestureTracker.direction(p1, p2));
        }

        try (Point p1 = new Point(0, 0); Point p2 = new Point(0, -1);) {
            assertEquals(Direction.Up, HeadGestureTracker.direction(p1, p2));
        }
    }

    @Test
    public void testDirectionMap() {
        Map<Direction, Float> directions = new EnumMap<>(Direction.class);
        Map<Direction, Integer> weights = new EnumMap<>(Direction.class);
        HeadGestureTracker.addDirection(directions, weights, Direction.None);
        assertEquals(0, directions.size());

        HeadGestureTracker.addDirection(directions, weights, Direction.Right, 20);
        assertEquals(1, directions.size());

        HeadGestureTracker.addDirection(directions, weights, Direction.Right, 20);
        assertEquals(1, directions.size());
        assertEquals(40, directions.get(Direction.Right).intValue());

        HeadGestureTracker.addDirection(directions, weights, Direction.Left);
        assertEquals(2, directions.size());
        assertEquals(40, directions.get(Direction.Right).intValue());
        assertEquals(1, directions.get(Direction.Left).intValue());

        assertEquals(Direction.Right, HeadGestureTracker.direction(directions, weights, DIRECTION_SIZE_AT_640));

        // Values must be very high to compensate dominating direction
        HeadGestureTracker.addDirection(directions, weights, Direction.Left, 1499);
        assertEquals(2, directions.size());
        assertEquals(40, directions.get(Direction.Right).intValue());
        assertEquals(1500, directions.get(Direction.Left).intValue());

        assertEquals(Direction.Left, HeadGestureTracker.direction(directions, weights, DIRECTION_SIZE_AT_640));

        // Values must be very high to compensate dominating direction
        HeadGestureTracker.addDirection(directions, weights, Direction.Up, 5000);
        assertEquals(3, directions.size());

        // Values must be very high to compensate dominating direction
        HeadGestureTracker.addDirection(directions, weights, Direction.Down, 5000);
        assertEquals(4, directions.size());
        assertEquals(5000, directions.get(Direction.Up).intValue());
        assertEquals(5000, directions.get(Direction.Down).intValue());

        // Direction.None because the direction is not distinct
        assertTrue(HeadGestureTracker.direction(directions, weights, DIRECTION_SIZE_AT_640) == Direction.None);

        HeadGestureTracker.addDirection(directions, weights, Direction.Down);
        assertEquals(4, directions.size());
        assertEquals(40, directions.get(Direction.Right).intValue());
        assertEquals(1500, directions.get(Direction.Left).intValue());
        assertEquals(5000, directions.get(Direction.Up).intValue());
        assertEquals(5001, directions.get(Direction.Down).intValue());

        assertEquals(Direction.None, HeadGestureTracker.direction(directions, weights, DIRECTION_SIZE_AT_640));

        HeadGestureTracker.addDirection(directions, weights, Direction.Up, 200000);
        assertEquals(4, directions.size());
        assertEquals(205000, directions.get(Direction.Up).intValue());
        assertEquals(5001, directions.get(Direction.Down).intValue());

        assertEquals(Direction.Up, HeadGestureTracker.direction(directions, weights, DIRECTION_SIZE_AT_640));
    }

    @Test
    public void testDirectionWeights() {
        Map<Direction, Float> directions = new EnumMap<>(Direction.class);
        Map<Direction, Integer> weights = new EnumMap<>(Direction.class);
        HeadGestureTracker.addDirection(directions, weights, Direction.None);
        assertEquals(0, directions.size());

        HeadGestureTracker.addDirection(directions, weights, Direction.Right, 50);
        HeadGestureTracker.addDirection(directions, weights, Direction.Right, 50);
        assertEquals(2, weights.get(Direction.Right).intValue());
        HeadGestureTracker.addDirection(directions, weights, Direction.Left, 75);
        assertEquals(1, weights.get(Direction.Left).intValue());

        assertEquals(100, directions.get(Direction.Right).intValue());
        assertEquals(75, directions.get(Direction.Left).intValue());
        assertEquals(Direction.Left, HeadGestureTracker.direction(directions, weights, DIRECTION_SIZE_AT_640));
    }

    @Test
    public void testOptimalShake() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testOptimalNod() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);

        assertEquals(Gesture.Nod, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodFollowedDirectlyByOptimalShake() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;

        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);

        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        // Fails because there's no pause between nodding and shaking
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodPauseShakeOptimal() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;

        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);

        i += 4;

        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        // Succeeds because there's a a timestamp gap before the shake
        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodShakeWithoutPause() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;

        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);

        i += 4;

        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        // Succeeds because there's a long shake after the node
        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodShakeWithInjection() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;

        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);

        directionTimeLine.add(Direction.None, SHAKETIME * i++);

        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        // None, since the illegal injection of Direction.None blocks recognition until it leaves the time slices window
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));

        // Needs a long time to re-adjust
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        // Succeeds because there's a long pause after the shake
        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodDistortedBySingleDirection() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);

        assertEquals(Gesture.Nod, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodDistortedByFullShake() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);

        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));

        i += 4;

        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);
        directionTimeLine.add(Direction.Down, SHAKETIME * i++);
        directionTimeLine.add(Direction.Up, SHAKETIME * i++);

        assertEquals(Gesture.Nod, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodWithIllegalInjection() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.None, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        // None, since the nod was interrupted by the illegal injection of Direction.None
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));

        i += 4;

        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testShakeWithPauseAndInvertedDirection() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= NUMBER_OF_SUPPORTED_DIRECTIONS);

        int i = 1;
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));

        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testMaxDurationTimeout() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= NUMBER_OF_SUPPORTED_DIRECTIONS);

        int i = 1;
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        directionTimeLine.add(Direction.Left, SHAKETIME * i++);

        i += 5;
        assertTrue(SHAKETIME * i >= HeadGestureTracker.GestureMaxDuration);

        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
    }

}
