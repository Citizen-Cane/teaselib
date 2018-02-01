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
        HeadGestureTracker.addDirection(directions, Direction.None);
        assertEquals(0, directions.size());

        HeadGestureTracker.addDirection(directions, Direction.Right, 15);
        assertEquals(1, directions.size());

        HeadGestureTracker.addDirection(directions, Direction.Right, 15);
        assertEquals(1, directions.size());
        assertEquals(30, directions.get(Direction.Right).intValue());

        HeadGestureTracker.addDirection(directions, Direction.Left);
        assertEquals(2, directions.size());
        assertEquals(30, directions.get(Direction.Right).intValue());
        assertEquals(1, directions.get(Direction.Left).intValue());

        assertEquals(Direction.Right, HeadGestureTracker.direction(directions));

        HeadGestureTracker.addDirection(directions, Direction.Left, 149);
        assertEquals(2, directions.size());
        assertEquals(30, directions.get(Direction.Right).intValue());
        assertEquals(150, directions.get(Direction.Left).intValue());

        assertEquals(Direction.Left, HeadGestureTracker.direction(directions));

        HeadGestureTracker.addDirection(directions, Direction.Up, 500);
        assertEquals(3, directions.size());

        HeadGestureTracker.addDirection(directions, Direction.Down, 500);
        assertEquals(4, directions.size());
        assertEquals(500, directions.get(Direction.Up).intValue());
        assertEquals(500, directions.get(Direction.Down).intValue());

        // Direction.None because the direction is not distinct
        assertTrue(HeadGestureTracker.direction(directions) == Direction.None);

        HeadGestureTracker.addDirection(directions, Direction.Down);
        assertEquals(4, directions.size());
        assertEquals(30, directions.get(Direction.Right).intValue());
        assertEquals(150, directions.get(Direction.Left).intValue());
        assertEquals(500, directions.get(Direction.Up).intValue());
        assertEquals(501, directions.get(Direction.Down).intValue());

        assertEquals(Direction.None, HeadGestureTracker.direction(directions));

        HeadGestureTracker.addDirection(directions, Direction.Up, 2000);
        assertEquals(4, directions.size());
        assertEquals(2500, directions.get(Direction.Up).intValue());
        assertEquals(501, directions.get(Direction.Down).intValue());

        assertEquals(Direction.Up, HeadGestureTracker.direction(directions));
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
    public void testMidMoveShake() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;

        // TODO This makes the test fail -> fix gesture tracking
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

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testSingleDistortedNod() {
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
    public void testNodMixedWithSingleShake() {
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
    }

    @Test
    public void testNodWithPause() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        int i = 1;
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.None, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.None, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodWithPauseAndRevertedDirection() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= NUMBER_OF_SUPPORTED_DIRECTIONS);

        int i = 1;
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.None, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.None, SHAKETIME * i++);
        directionTimeLine.add(Direction.Left, SHAKETIME * i++);
        directionTimeLine.add(Direction.Right, SHAKETIME * i++);

        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
    }
}
