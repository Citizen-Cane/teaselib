package teaselib.core.devices.motiondetection;

import static teaselib.core.javacv.util.Geom.*;
import static teaselib.core.javacv.util.Gui.rectangles;

import java.util.LinkedList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Rect;

public class RegionHistory {
    private final LinkedList<Rect> history;
    private final int capacity;

    public RegionHistory(int capacity) {
        this.history = new LinkedList<>();
        this.capacity = capacity;
    }

    public void clear() {
        history.clear();
    }

    public void add(MatVector contours) {
        add(rectangles(contours));
    }

    public void add(List<Rect> contours) {
        if (contours.size() == 0) {
            // No motion - no update
            history.add(tail());
        } else {
            if (history.size() == capacity) {
                history.poll();
            }
            history.add(join(contours));
        }
    }

    public int size() {
        return history.size();
    }

    public Rect tail() {
        return history.getLast();
    }

    public Rect region() {
        return join(history);
    }
}
