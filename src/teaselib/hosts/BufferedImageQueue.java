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

    // TODO avoid flicker caused by slow EventQueue.invokeLater(() -> show(image))
    // when awt is too slow to render the image, it will be reused and repainted while being rendered to surface
    private static final int BUFFER_CAPACITY = 10;

    final int capacity;
    final Deque<BufferedImage> buffers = new ArrayDeque<>(BUFFER_CAPACITY);

    BufferedImageQueue(int capacity) {
        this.capacity = capacity;
    }

    public BufferedImage nextBuffer(Rectangle bounds) {
        BufferedImage image;
        if (buffers.size() >= BUFFER_CAPACITY) {
            image = newOrSameImage(buffers.remove(), bounds);
        } else {
            image = newImage(bounds);
        }
        buffers.add(image);
        return image;
    }

    private GraphicsConfiguration gc;

    public void setGraphicsConfiguration(GraphicsConfiguration gc) {
        if (gc != this.gc) {
            buffers.clear();
        }
        this.gc = gc;
    }

    protected BufferedImage newOrSameImage(BufferedImage image, Rectangle bounds) {
        if (image == null) {
            return newImage(bounds);
        } else if (bounds.width != image.getWidth() || bounds.height != image.getHeight()) {
            return newImage(bounds);
        } else {
            return image;
        }
    }

    public BufferedImage newImage(Rectangle bounds) {
        return newImage(bounds.width, bounds.height);
    }

    private BufferedImage newImage(int width, int height) {
        if (gc != null) {
            return gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        } else {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

}
