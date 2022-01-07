package teaselib.hosts;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Optional;

public class Transform {

    private Transform() {
    }

    /**
     * Maximize image size within the bounds while maintaining the same size for landscape and portrait images:
     * <p>
     * The transform assumes that all images in a set are made made with the same resolution.
     * <p>
     * Fill the bounds so that for landscape bounds
     * <li>landscape images fill the bounds horizontally
     * <li>portrait images fill the bounds as if the image was landscape.
     * <p>
     * Fill the bounds so that for portrait bounds
     * <li>landscape images fill the bounds as if the image was portrait.
     * <li>portrait images fill the bounds vertically
     * <p>
     * This way each set can define the actual actor distance and size. Most importantly for image sets with similar
     * subject sizes the size will be the same for both landsacpe and portrait images.
     * <p>
     * Then, the image is zoomed towards the focus area according to the player distance, and translated to avoid the
     * focus region covered by any overlays (namely the text area)
     * <p>
     * 
     * 
     * @param image
     * @param bounds
     *            Bounding box
     * @return
     */
    static AffineTransform maxImage(BufferedImage image, Dimension bounds, Optional<Rectangle.Double> focusArea) {
        return maxImage(dimension(image), bounds, focusArea);
    }

    static AffineTransform maxImage(Dimension imageSize, Dimension bounds, Optional<Rectangle.Double> focusArea) {
        // transforms are concatenated from bottom to top
        var t = new AffineTransform();

        t.translate(0.5 * bounds.width, 0.5 * bounds.height);
        // translate image center to bounds center

        var size = bounds.getSize();
        double aspect;
        if (focusArea.isPresent()) {
            // fill screen estate while retaining same dpi for landscape and portrait images
            if (aspect(size) > 1.0) {
                aspect = fitOutside(aspect(imageSize) >= 1.0 ? imageSize : swap(imageSize), size);
            } else {
                aspect = fitOutside(aspect(imageSize) < 1.0 ? imageSize : swap(imageSize), size);
            }
        } else {
            // show the whole image
            aspect = fitInside(imageSize, size);
        }
        t.scale(aspect, aspect);
        // scale to user space

        t.scale(imageSize.getWidth(), imageSize.getHeight());
        // scale back to image space

        t.translate(-0.5, -0.5);
        // move image center to 0,0

        t.scale(1.0 / imageSize.getWidth(), 1.0 / imageSize.getHeight());
        // transform to normalized

        return t;
    }

    /**
     * adjust image position so that the transformed focus area is inside the bounds
     * 
     * @return
     */
    public static AffineTransform keepFocusAreaVisible(AffineTransform surface, Dimension image, Rectangle bounds,
            Rectangle2D.Double focus) {
        var keepFocusAreaVisible = new AffineTransform();

        Point2D top = surface.transform(new Point2D.Double(focus.getCenterX(), focus.getMinY()), new Point2D.Double());
        if (!bounds.contains(top)) {
            Point2D offset = surface.transform(new Point2D.Double(image.getWidth() / 2.0, 0.0), new Point2D.Double());
            keepFocusAreaVisible.translate(0.0, -offset.getY());
        } else {
            Point2D bottom = surface.transform(new Point2D.Double(focus.getCenterX(), focus.getMaxY()),
                    new Point2D.Double());
            if (!bounds.contains(bottom)) {
                Point2D offset = surface.transform(new Point2D.Double(image.getWidth() / 2.0, image.getHeight()),
                        new Point2D.Double());
                keepFocusAreaVisible.translate(0.0, bottom.getY() - offset.getY());
            } else {
                Point2D left = surface.transform(new Point2D.Double(focus.getMinX(), focus.getCenterY()),
                        new Point2D.Double());
                if (!bounds.contains(left)) {
                    Point2D offset = surface.transform(new Point2D.Double(0.0, image.getHeight() / 2.0),
                            new Point2D.Double());
                    keepFocusAreaVisible.translate(-offset.getX(), 0.0);
                } else {
                    Point2D right = surface.transform(new Point2D.Double(focus.getMaxX(), focus.getCenterY()),
                            new Point2D.Double());
                    if (!bounds.contains(right)) {
                        Point2D offset = surface.transform(
                                new Point2D.Double(image.getWidth(), image.getHeight() / 2.0), new Point2D.Double());
                        keepFocusAreaVisible.translate(right.getX() - offset.getX(), 0.0);
                    }
                }
            }
        }

        keepFocusAreaVisible.preConcatenate(surface);
        return keepFocusAreaVisible;
    }

    public static AffineTransform zoom(AffineTransform t, Rectangle2D.Double focusArea, double zoom) {
        var zoomed = new AffineTransform();

        Point2D focus = new Point2D.Double(focusArea.getCenterX(), focusArea.getCenterY());
        t.translate(focus.getX(), focus.getY());
        t.scale(zoom, zoom);
        t.translate(-focus.getX(), -focus.getY());

        zoomed.concatenate(t);
        return zoomed;
    }

    public static void avoidFocusAreaBehindText(AffineTransform surface, Rectangle2D.Double focusArea, int textAreaX) {
        Point2D focusRight = surface.transform(new Point2D.Double(focusArea.getMaxX(), focusArea.getCenterY()),
                new Point2D.Double());
        double overlap = focusRight.getX() - textAreaX;
        if (overlap > 0) {
            surface.preConcatenate(AffineTransform.getTranslateInstance(-overlap, 0));
        }
    }

    /**
     * The scale factor to fill the bounding box
     * 
     * @return A double value to scale the image onto the bounding box.
     */
    static double fitOutside(Dimension size, Dimension onto) {
        return Math.max((double) onto.width / size.width, (double) onto.height / size.height);
    }

    /**
     * Center the image completely inside the box, leaving borders in one direction.
     * 
     * @return A double value to scale the image into the bounding box.
     */
    static double fitInside(Dimension size, Dimension into) {
        return Math.min((double) into.width / size.width, (double) into.height / size.height);
    }

    /**
     * Return the aspect of the given size.
     * 
     * @param size
     * @return How much dimension is wider than high
     */
    static double aspect(Dimension size) {
        return (double) size.width / size.height;
    }

    static Dimension dimension(BufferedImage image) {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    static Dimension swap(Dimension dimension) {
        return new Dimension(dimension.height, dimension.width);
    }

    static Rectangle2D.Double scale(Rectangle2D.Double r, Dimension scale) {
        return new Rectangle2D.Double(r.x * scale.width, r.y * scale.height, r.width * scale.width,
                r.height * scale.height);
    }

}
