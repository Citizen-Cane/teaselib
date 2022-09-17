package teaselib.host;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.geom.AffineTransform;

/**
 * @author Citizen-Cane
 *
 */
public abstract class AbstractValidatedImage<T extends Image> {

    public interface ImageSupplier<T> {
        T create(GraphicsConfiguration gc, int width, int height, int transparency);
    }

    protected interface Renderer {
        void paint(Graphics2D g2d);
    }

    final ImageSupplier<T> newImage;
    protected final int transparency;

    T image;
    protected int width;
    protected int height;
    protected boolean invalidated = true;

    AbstractValidatedImage(ImageSupplier<T> newImage, int transparency) {
        this.newImage = newImage;
        this.transparency = transparency;
        this.width = -1;
        this.height = -1;
        this.image = null;

    }

    abstract T get(GraphicsConfiguration gc);

    public abstract boolean contentsLost();

    abstract void draw(Graphics2D graphics, GraphicsConfiguration gc);

    abstract void draw(Graphics2D graphics, GraphicsConfiguration gc, AffineTransform t);

    abstract void paint(GraphicsConfiguration gc, Renderer renderer);

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    Dimension dimension() {
        return new Dimension(width, height);
    }

}
