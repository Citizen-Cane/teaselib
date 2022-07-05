package teaselib.hosts;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author admin
 *
 */
public class AbstractBufferedImageRenderer {

    final Deque<BufferedImage> buffers = new ArrayDeque<>(2);

    public static BufferedImage newOrSameImage(BufferedImage image, Rectangle bounds) {
        if (image == null) {
            return newImage(bounds);
        } else if (bounds.width != image.getWidth() || bounds.height != image.getHeight()) {
            return newImage(bounds);
        } else {
            return image;
        }
    }

    public BufferedImage nextBuffer(Rectangle bounds) {
        BufferedImage image;
        if (buffers.size() >= 2) {
            image = newOrSameImage(buffers.remove(), bounds);
        } else {
            image = newImage(bounds);
        }
        buffers.add(image);
        return image;
    }

    public static BufferedImage newImage(Rectangle bounds) {
        return newImage(bounds.width, bounds.height);
    }

    private static BufferedImage newImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * 
     */
    public AbstractBufferedImageRenderer() {
        super();
    }

}