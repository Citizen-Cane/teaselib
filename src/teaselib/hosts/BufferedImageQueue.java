package teaselib.hosts;

import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author Citizen-Cane
 *
 */
public class BufferedImageQueue {

    final int capacity;
    final Deque<BufferedImage> buffers;

    BufferedImageQueue(int capacity) {
        this.capacity = capacity;
        this.buffers = new ArrayDeque<>(capacity);
    }

    private BufferedImage acquireBuffer(GraphicsConfiguration gc, Rectangle bounds) {
        BufferedImage image;
        if (buffers.size() >= capacity) {
            image = newOrSameImage(gc, buffers.remove(), bounds);
        } else {
            image = newImage(gc, bounds);
        }
        return image;
    }

    public BufferedImage rotateBuffer(GraphicsConfiguration gc, Rectangle bounds) {
        var image = acquireBuffer(gc, bounds);
        buffers.add(image);
        return image;
    }

    protected BufferedImage newOrSameImage(GraphicsConfiguration gc, BufferedImage image, Rectangle bounds) {
        if (image == null) {
            return newImage(gc, bounds);
        } else if (bounds.width != image.getWidth() || bounds.height != image.getHeight()) {
            return newImage(gc, bounds);
        } else {
            return image;
        }
    }

    public static BufferedImage newImage(GraphicsConfiguration gc, Rectangle bounds) {
        return newImage(gc, bounds.width, bounds.height);
    }

    private static BufferedImage newImage(GraphicsConfiguration gc, int width, int height) {
        return gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    }

}
