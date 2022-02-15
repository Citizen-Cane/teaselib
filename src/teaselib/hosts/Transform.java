package teaselib.hosts;

import java.awt.Dimension;
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
    static AffineTransform maxImage(BufferedImage image, Rectangle2D bounds, Optional<Rectangle2D> focusArea) {
        return maxImage(dimension(image), bounds, focusArea);
    }

    static AffineTransform maxImage(Dimension image, Rectangle2D bounds, Optional<Rectangle2D> focusArea) {
        // transforms are concatenated from bottom to top
        var t = new AffineTransform();

        t.translate(0.5 * bounds.getWidth(), 0.5 * bounds.getHeight());
        // translate image center to bounds center

        Dimension size = bounds.getBounds().getSize();
        double aspect;
        if (focusArea.isPresent()) {
            // fill screen estate while retaining same dpi for landscape and portrait images
            if (aspect(size) > 1.0) {
                aspect = fitOutside(aspect(image) >= 1.0 ? image : swap(image), size);
            } else {
                aspect = fitOutside(aspect(image) < 1.0 ? image : swap(image), size);
            }
        } else {
            // show the whole image
            aspect = fitInside(image, size);
        }
        t.scale(aspect, aspect);
        // scale to user space

        t.scale(image.getWidth(), image.getHeight());
        // scale back to image space

        t.translate(-0.5, -0.5);
        // move image center to 0,0

        t.scale(1.0 / image.getWidth(), 1.0 / image.getHeight());
        // transform to normalized

        return t;
    }

    static final double goldenRatio = 1.618033988749;
    static final double goldenRatioFactorB = 2.0 - goldenRatio;

    /**
     * adjust image position so that the transformed focus area is inside the bounds
     * 
     * @return
     */
    public static AffineTransform keepFocusAreaVisible(AffineTransform surface, Dimension image, Rectangle2D bounds,
            Rectangle2D focusArea) {
        AffineTransform keepFocusAreaVisible;

        var rect = surface.createTransformedShape(focusArea).getBounds2D();
        Point2D imageTopLeft = surface.transform(new Point2D.Double(0.0, 0.0), new Point2D.Double());
        Point2D imageBottomRight = surface.transform(new Point2D.Double(image.getWidth(), image.getHeight()),
                new Point2D.Double());

        if (rect.getCenterY() < bounds.getMinY() + bounds.getHeight() * goldenRatioFactorB) {
            double expected = bounds.getHeight() * goldenRatioFactorB - rect.getCenterY();
            double maximal = bounds.getMinY() - imageTopLeft.getY();
            keepFocusAreaVisible = AffineTransform.getTranslateInstance(0.0, Math.min(maximal, expected));
        } else if (rect.getCenterY() > bounds.getMaxY() - bounds.getHeight() * goldenRatioFactorB) {
            double expected = bounds.getHeight() * goldenRatioFactorB - rect.getCenterY();
            double maximal = bounds.getMaxY() - imageBottomRight.getY();
            keepFocusAreaVisible = AffineTransform.getTranslateInstance(0.0, Math.max(maximal, expected));
        } else if (rect.getCenterX() < bounds.getMinX() + bounds.getWidth() * goldenRatioFactorB) {
            double expected = bounds.getWidth() * goldenRatioFactorB - rect.getCenterX();
            double maximal = bounds.getMinX() - imageTopLeft.getX();
            keepFocusAreaVisible = AffineTransform.getTranslateInstance(0.0, Math.min(maximal, expected));
        } else if (rect.getCenterX() > bounds.getMaxX() - bounds.getWidth() * goldenRatioFactorB) {
            double expected = bounds.getWidth() * goldenRatioFactorB - rect.getCenterX();
            double maximal = bounds.getMaxX() - imageBottomRight.getX();
            keepFocusAreaVisible = AffineTransform.getTranslateInstance(0.0, Math.max(maximal, expected));
        } else {
            return surface;
        }

        keepFocusAreaVisible.concatenate(surface);
        return keepFocusAreaVisible;
    }

    public static AffineTransform zoom(AffineTransform t, Rectangle2D focusArea, double zoom) {
        var zoomed = new AffineTransform();

        Point2D focus = new Point2D.Double(focusArea.getCenterX(), focusArea.getCenterY());
        t.translate(focus.getX(), focus.getY());
        t.scale(zoom, zoom);
        t.translate(-focus.getX(), -focus.getY());

        zoomed.concatenate(t);
        return zoomed;
    }

    public static void avoidFocusAreaBehindText(AffineTransform surface, Dimension image, Rectangle2D bounds,
            Rectangle2D focusArea, int textAreaX) {
        Point2D focusRight = surface.transform(new Point2D.Double(focusArea.getMaxX(), focusArea.getCenterY()),
                new Point2D.Double());

        Point2D imageLeft = surface.transform(new Point2D.Double(0, 0), new Point2D.Double());
        Point2D imageRight = surface.transform(new Point2D.Double(image.getWidth(), image.getHeight()),
                new Point2D.Double());

        double overlap = focusRight.getX() - textAreaX;
        double maxTranslate = Math.max(imageRight.getX() - bounds.getMaxX(), imageLeft.getX());
        if (overlap > 0 && maxTranslate > 0) {
            double compensation = Math.min(overlap, maxTranslate);
            surface.preConcatenate(AffineTransform.getTranslateInstance(-compensation, 0));
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

    static Rectangle2D.Double scale(Rectangle2D r, Dimension scale) {
        return new Rectangle2D.Double(r.getMinX() * scale.width, r.getMinY() * scale.height, r.getWidth() * scale.width,
                r.getHeight() * scale.height);
    }

}
