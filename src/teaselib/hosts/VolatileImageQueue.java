package teaselib.hosts;

import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.VolatileImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author Citizen-Cane
 *
 */
public class VolatileImageQueue {

    final int capacity;
    final Deque<VolatileImage> buffers;

    VolatileImageQueue(int capacity) {
        this.capacity = capacity;
        this.buffers = new ArrayDeque<>(capacity);
    }

    private VolatileImage acquireBuffer(GraphicsConfiguration gc, Rectangle bounds) {
        VolatileImage image;
        if (buffers.size() >= capacity) {
            image = newOrSameImage(gc, buffers.remove(), bounds);
        } else {
            image = newImage(gc, bounds);
        }
        return image;
    }

    public VolatileImage rotateBuffer(GraphicsConfiguration gc, Rectangle bounds) {
        var image = acquireBuffer(gc, bounds);
        buffers.add(image);
        return image;
    }

    protected VolatileImage newOrSameImage(GraphicsConfiguration gc, VolatileImage image, Rectangle bounds) {
        if (image == null) {
            return newImage(gc, bounds);
        } else if (bounds.width != image.getWidth() || bounds.height != image.getHeight()) {
            return newImage(gc, bounds);
        } else if (image.validate(gc) != VolatileImage.IMAGE_OK) {
            return newImage(gc, bounds);
        } else {
            return image;
        }
    }

    public static VolatileImage newImage(GraphicsConfiguration gc, Rectangle bounds) {
        return newImage(gc, bounds.width, bounds.height);
    }

    private static VolatileImage newImage(GraphicsConfiguration gc, int width, int height) {
        return gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
    }

}
