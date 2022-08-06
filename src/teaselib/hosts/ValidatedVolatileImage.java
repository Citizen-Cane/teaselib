package teaselib.hosts;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.VolatileImage;

/**
 * @author Citizen-Cane
 *
 */
public class ValidatedVolatileImage {

    interface ImageSupplier {
        VolatileImage create(GraphicsConfiguration gc, int width, int height, int transparency);
    }

    interface Renderer {
        void paint(Graphics2D g2d);
    }

    private final ImageSupplier newImage;
    private final Renderer renderer;
    private final int transparency;

    private VolatileImage image;
    private int width;
    private int height;

    public ValidatedVolatileImage(Renderer renderer, int transparency) {
        this((gc, w, h, t) -> gc.createCompatibleVolatileImage(w, h, t), renderer, transparency);
    }

    public ValidatedVolatileImage(ImageSupplier newImage, Renderer renderer, int transparency) {
        this.newImage = newImage;
        this.renderer = renderer;
        this.width = -1;
        this.height = -1;
        this.transparency = transparency;
        this.image = null;
    }

    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    VolatileImage get(GraphicsConfiguration gc, int width, int height) {
        setSize(width, height);
        return get(gc);
    }

    VolatileImage get(GraphicsConfiguration gc) {
        if (image == null) {
            image = newImage.create(gc, width, height, transparency);
            repaint(image);
        } else if (width != image.getWidth()
                || height != image.getHeight()) {
            image = newImage.create(gc, width, height, transparency);
            repaint(image);
        } else {
            int result = image.validate(gc);
            if (result == VolatileImage.IMAGE_RESTORED) {
                repaint(image);
            } else if (result == VolatileImage.IMAGE_INCOMPATIBLE) {
                image = newImage.create(gc, width, height, transparency);
                repaint(image);
            }
        }
        return image;
    }

    private void repaint(VolatileImage img) {
        var g2d = img.createGraphics();
        renderer.paint(g2d);
        g2d.dispose();
    }

    public void repaint(GraphicsConfiguration gc) {
        var current = image;
        var newImage = get(gc);
        boolean repaintRequired = newImage == current;
        if (repaintRequired) {
            repaint(image);
        }
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    public Dimension dimension() {
        return new Dimension(width, height);
    }

    public boolean contentsLost() {
        return image != null && image.contentsLost();
    }

}
