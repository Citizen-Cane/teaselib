package teaselib.hosts;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * @author Citizen-Cane
 *
 */
public class ValidatedBufferedImage extends AbstractValidatedImage<BufferedImage> {

    public ValidatedBufferedImage(BufferedImage image) {
        this((gc, w, h, t) -> {
            throw new UnsupportedOperationException("image size change");
        }, image.getTransparency());
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        invalidated = false;
    }

    public ValidatedBufferedImage(ImageSupplier<BufferedImage> newImage, int transparency) {
        super(newImage, transparency);
    }

    @Override
    public boolean contentsLost() {
        return false;
    }

    @Override
    protected BufferedImage get(GraphicsConfiguration gc) {
        if (image == null) {
            image = newImage.create(gc, width, height, transparency);
            invalidated = true;
        } else if (width != image.getWidth()
                || height != image.getHeight()) {
            image = newImage.create(gc, width, height, transparency);
            invalidated = true;
        }
        return image;
    }

    @Override
    public void paint(GraphicsConfiguration gc, Renderer renderer) {
        get(gc);
        var g2d = image.createGraphics();
        renderer.paint(g2d);
        g2d.dispose();
        invalidated = false;
    }

    @Override
    public void draw(Graphics2D graphics, GraphicsConfiguration gc, AffineTransform t) {
        if (!invalidated) {
            graphics.drawImage(image, t, null);
        }
    }

}
