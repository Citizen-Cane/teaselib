package teaselib.hosts;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

/**
 * @author Citizen-Cane
 *
 */
public class ValidatedVolatileImage extends AbstractValidatedImage<VolatileImage> {

    private final BufferedImage backBuffer;

    public ValidatedVolatileImage(int transparency) {
        this((gc, w, h, t) -> gc.createCompatibleVolatileImage(w, h, t), transparency);
    }

    public ValidatedVolatileImage(BufferedImage backBuffer) {
        this((gc, w, h, t) -> gc.createCompatibleVolatileImage(w, h, t), backBuffer.getTransparency(), backBuffer);
    }

    public ValidatedVolatileImage(ImageSupplier<VolatileImage> newImage, int transparency) {
        this(newImage, transparency, null);
    }

    public ValidatedVolatileImage(ImageSupplier<VolatileImage> newImage, int transparency, BufferedImage backBuffer) {
        super(newImage, transparency);
        this.backBuffer = backBuffer;
        if (backBuffer != null) {
            this.width = backBuffer.getWidth();
            this.height = backBuffer.getHeight();
        }
    }

    @Override
    protected VolatileImage get(GraphicsConfiguration gc) {
        if (image == null) {
            image = newImage.create(gc, width, height, transparency);
            invalidated = true;
        } else if (width != image.getWidth()
                || height != image.getHeight()) {
            image = newImage.create(gc, width, height, transparency);
            invalidated = true;
        } else {
            int result = image.validate(gc);
            if (result == VolatileImage.IMAGE_RESTORED) {
                invalidated = true;
            } else if (result == VolatileImage.IMAGE_INCOMPATIBLE) {
                image = newImage.create(gc, width, height, transparency);
                invalidated = true;
            } else {
                return image;
            }
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
        get(gc);
        if (invalidated && backBuffer != null) {
            paint(gc, g2d -> g2d.drawImage(backBuffer, 0, 0, null));
            invalidated = false;
        } else if (!invalidated) {
            graphics.drawImage(image, t, null);
        }
    }

    @Override
    public boolean contentsLost() {
        return image != null && image.contentsLost();
    }

}
