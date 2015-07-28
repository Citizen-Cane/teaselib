/**
 * 
 */
package teaselib.motiondetection;

import java.util.LinkedList;

/**
 * @author someone
 *
 */
public class MotionHistory {
    private final int frames;

    private final LinkedList<Double> values = new LinkedList<Double>();

    public MotionHistory(int frames) {
        super();
        this.frames = frames;
    }

    public void clear() {
        values.clear();
    }

    public void add(double value) {
        values.addLast(value);
        if (values.size() > frames) {
            values.removeFirst();
        }
    }

    /**
     * Return the maximum motion value over the last n frames
     * 
     * @param pastFrames
     *            The number of frames
     * @return The maximum motion value
     */
    public double getMotion(int pastFrames) {
        double max = 0;
        final int size = values.size();
        for (int i = Math.max(0, size - pastFrames); i < size; i++) {
            double v = values.get(i);
            if (v > max) {
                max = v;
            }
        }
        return max;
    }
}
