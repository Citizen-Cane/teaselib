package teaselib.core.javacv;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Point;
import org.junit.Test;

import teaselib.core.util.TimeLine;
import teaselib.motiondetection.Gesture;

public class HeadGestureTrackerTest {
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
        Map<Direction, Integer> directions = new EnumMap<>(Direction.class);
        HeadGestureTracker.addDirection(directions, Direction.None);
        assertEquals(0, directions.size());

        HeadGestureTracker.addDirection(directions, Direction.Right);
        assertEquals(1, directions.size());

        HeadGestureTracker.addDirection(directions, Direction.Right);
        assertEquals(1, directions.size());
        assertEquals(2, directions.get(Direction.Right).intValue());

        HeadGestureTracker.addDirection(directions, Direction.Left);
        assertEquals(2, directions.size());
        assertEquals(2, directions.get(Direction.Right).intValue());
        assertEquals(1, directions.get(Direction.Left).intValue());

        assertEquals(Direction.Right, HeadGestureTracker.direction(directions));

        HeadGestureTracker.addDirection(directions, Direction.Left, 3);
        assertEquals(2, directions.size());
        assertEquals(2, directions.get(Direction.Right).intValue());
        assertEquals(4, directions.get(Direction.Left).intValue());

        assertEquals(Direction.Left, HeadGestureTracker.direction(directions));

        HeadGestureTracker.addDirection(directions, Direction.Up, 5);
        assertEquals(3, directions.size());

        HeadGestureTracker.addDirection(directions, Direction.Down, 5);
        assertEquals(4, directions.size());
        assertEquals(5, directions.get(Direction.Up).intValue());
        assertEquals(5, directions.get(Direction.Down).intValue());

        assertTrue(HeadGestureTracker.direction(directions) == Direction.Up
                || HeadGestureTracker.direction(directions) == Direction.Down);

        HeadGestureTracker.addDirection(directions, Direction.Down);
        assertEquals(4, directions.size());
        assertEquals(2, directions.get(Direction.Right).intValue());
        assertEquals(4, directions.get(Direction.Left).intValue());
        assertEquals(5, directions.get(Direction.Up).intValue());
        assertEquals(6, directions.get(Direction.Down).intValue());

        assertEquals(Direction.Down, HeadGestureTracker.direction(directions));

        HeadGestureTracker.addDirection(directions, Direction.Up, 3);
        assertEquals(4, directions.size());
        assertEquals(8, directions.get(Direction.Up).intValue());
        assertEquals(6, directions.get(Direction.Down).intValue());

        assertEquals(Direction.Up, HeadGestureTracker.direction(directions));
    }

    @Test
    public void testOptimalShake() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        long shakeTime = (HeadGestureTracker.GestureMinDuration + HeadGestureTracker.GestureMaxDuration)
                / (2 * HeadGestureTracker.NumberOfDirections);
        directionTimeLine.add(Direction.Right, shakeTime * 1);
        directionTimeLine.add(Direction.Left, shakeTime * 2);
        directionTimeLine.add(Direction.Right, shakeTime * 3);
        directionTimeLine.add(Direction.Left, shakeTime * 4);
        directionTimeLine.add(Direction.Right, shakeTime * 5);
        directionTimeLine.add(Direction.Left, shakeTime * 6);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testOptimalNod() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 4);

        long shakeTime = (HeadGestureTracker.GestureMinDuration + HeadGestureTracker.GestureMaxDuration)
                / (4 * HeadGestureTracker.NumberOfDirections);
        directionTimeLine.add(Direction.Down, shakeTime * 1);
        directionTimeLine.add(Direction.Up, shakeTime * 2);
        directionTimeLine.add(Direction.Down, shakeTime * 3);
        directionTimeLine.add(Direction.Up, shakeTime * 4);

        assertEquals(Gesture.Nod, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testMidMoveShake() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 6);

        long shakeTime = (HeadGestureTracker.GestureMinDuration + HeadGestureTracker.GestureMaxDuration)
                / (HeadGestureTracker.NumberOfDirections);
        directionTimeLine.add(Direction.Up, shakeTime * 1);
        directionTimeLine.add(Direction.Down, shakeTime * 2);
        directionTimeLine.add(Direction.Up, shakeTime * 3);
        directionTimeLine.add(Direction.Down, shakeTime * 4);

        directionTimeLine.add(Direction.Right, shakeTime * 5);
        directionTimeLine.add(Direction.Left, shakeTime * 6);
        directionTimeLine.add(Direction.Right, shakeTime * 7);
        directionTimeLine.add(Direction.Left, shakeTime * 8);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testSingleDistortedNod() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 4);

        long shakeTime = (HeadGestureTracker.GestureMinDuration + HeadGestureTracker.GestureMaxDuration)
                / (4 * HeadGestureTracker.NumberOfDirections);
        directionTimeLine.add(Direction.Down, shakeTime * 1);
        directionTimeLine.add(Direction.Up, shakeTime * 2);
        directionTimeLine.add(Direction.Left, shakeTime * 3);
        directionTimeLine.add(Direction.Down, shakeTime * 4);
        directionTimeLine.add(Direction.Up, shakeTime * 5);

        assertEquals(Gesture.Nod, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodMixedWithSingleShake() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 4);

        long shakeTime = (HeadGestureTracker.GestureMinDuration + HeadGestureTracker.GestureMaxDuration)
                / (4 * HeadGestureTracker.NumberOfDirections);
        directionTimeLine.add(Direction.Down, shakeTime * 1);
        directionTimeLine.add(Direction.Up, shakeTime * 2);
        directionTimeLine.add(Direction.Left, shakeTime * 3);
        directionTimeLine.add(Direction.Down, shakeTime * 4);
        directionTimeLine.add(Direction.Right, shakeTime * 5);
        directionTimeLine.add(Direction.Up, shakeTime * 6);

        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodWithPause() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 4);

        long shakeTime = (HeadGestureTracker.GestureMinDuration + HeadGestureTracker.GestureMaxDuration)
                / (4 * HeadGestureTracker.NumberOfDirections);
        directionTimeLine.add(Direction.Left, shakeTime * 1);
        directionTimeLine.add(Direction.Right, shakeTime * 2);
        directionTimeLine.add(Direction.None, shakeTime * 3);
        directionTimeLine.add(Direction.Left, shakeTime * 4);
        directionTimeLine.add(Direction.Right, shakeTime * 5);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }

    @Test
    public void testNodWithPauseAndRevertedDirection() {
        TimeLine<Direction> directionTimeLine = new TimeLine<>(0);
        assertEquals(Gesture.None, HeadGestureTracker.getGesture(directionTimeLine));
        assertTrue(HeadGestureTracker.NumberOfDirections <= 4);

        long shakeTime = (HeadGestureTracker.GestureMinDuration + HeadGestureTracker.GestureMaxDuration)
                / (4 * HeadGestureTracker.NumberOfDirections);
        directionTimeLine.add(Direction.Left, shakeTime * 1);
        directionTimeLine.add(Direction.Right, shakeTime * 2);
        directionTimeLine.add(Direction.None, shakeTime * 3);
        directionTimeLine.add(Direction.Right, shakeTime * 4);
        directionTimeLine.add(Direction.Left, shakeTime * 5);

        assertEquals(Gesture.Shake, HeadGestureTracker.getGesture(directionTimeLine));
    }
}
