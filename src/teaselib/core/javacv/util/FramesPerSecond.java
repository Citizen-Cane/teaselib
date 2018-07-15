package teaselib.core.javacv.util;

import java.util.ArrayList;
import java.util.List;

public class FramesPerSecond {
    private final int capacity;
    // TODO use TimeLine or linked list
    private List<Long> frameTimes;

    double fps = 0.0;
    long frameStartTime = 0;

    /**
     * Find the number closest to the desired frames per second.
     * 
     * @param capableFps
     *            What the device can provide
     * @param desiredFps
     *            What the application wants
     * @return A fps value so that for all integers n * returnedFps =< capableFPS
     */
    public static double getFps(double capableFps, double desiredFps) {
        final double fps;
        if (capableFps >= desiredFps * 2) {
            double div = Math.floorDiv((int) capableFps, (int) desiredFps);
            fps = capableFps / div;
        } else if (capableFps > desiredFps) {
            fps = capableFps / 2.0;
        } else {
            fps = desiredFps;
        }
        return fps;
    }

    public FramesPerSecond(int capacity) {
        this.capacity = capacity;
        frameTimes = new ArrayList<>(capacity);
    }

    public void start() {
        frameStartTime = System.currentTimeMillis();
    }

    public void updateFrame() {
        updateFrame(System.currentTimeMillis());
    }

    public void updateFrame(long currentTimeMillis) {
        long frameDuration = currentTimeMillis - frameStartTime;
        frameTimes.add(frameDuration);
        if (frameTimes.size() == capacity) {
            double averageFrameTime = averageFrameTime(frameTimes);
            if (averageFrameTime > 0) {
                fps = 1000.0 / averageFrameTime;
            } else {
                fps = 0.0;
            }
            frameTimes.remove(0);
        }
        frameStartTime = currentTimeMillis;
    }

    private double averageFrameTime(List<Long> list) {
        long value = 0;
        for (Long d : list) {
            value += d;
        }
        value /= capacity;
        return value;
    }

    public double value() {
        return fps;
    }

    public long timeMillisLeft(long desiredFrameTime) {
        return timeMillisLeft(desiredFrameTime, System.currentTimeMillis());
    }

    public long timeMillisLeft(long desiredFrameTime, long now) {
        return Math.max(0, frameStartTime + desiredFrameTime - now);
    }

    public long frameTime() {
        if (frameTimes.isEmpty()) {
            return 0;
        } else {
            return frameTimes.get(frameTimes.size() - 1);
        }
    }
}
