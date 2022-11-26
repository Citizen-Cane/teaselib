package teaselib.host;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.VolatileImage;
import java.util.Optional;

public class Transform {

    public interface FitFunction {
        double apply(Dimension image, Dimension bounds);
    }

    public interface FocusFunction {
        AffineTransform apply(AffineTransform surface, Dimension image, Rectangle2D bounds, Rectangle2D imageFocusArea);
    }

    private Transform() {}

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
    static AffineTransform maxImage(Dimension image, Rectangle2D bounds, Optional<Rectangle2D> focusRegion,
            FitFunction fitFunction) {
        // transforms are concatenated from bottom to top
        var t = new AffineTransform();

        t.translate(0.5 * bounds.getWidth(), 0.5 * bounds.getHeight());
        // translate image center to bounds center

        Dimension size = bounds.getBounds().getSize();
        double aspect;
        if (focusRegion.isPresent()) {
            aspect = fitFunction.apply(image, size);
            // fit outside or inside or attempt to zoom in a little more
            // in order fill the screen without cropping
        } else {
            aspect = fitInside(image, size);
            // show the whole image
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
    static final double goldenRatioFactorA = goldenRatio - 1.0;
    static final double goldenRatioFactorB = 2.0 - goldenRatio;

    /**
     * Adjust image so that the focus area is completely inside the visible bounds and its center matches the golden
     * ratio.
     * 
     * @return AffineTransform
     */
    static AffineTransform matchGoldenRatioOrKeepVisible(AffineTransform surface, Dimension image, Rectangle2D bounds, Rectangle2D imageFocusArea) {
        var surfaceFocusArea = surface.createTransformedShape(imageFocusArea).getBounds2D();
        Point2D imageTopLeft = surface.transform(new Point2D.Double(0.0, 0.0), new Point2D.Double());
        Point2D imageBottomRight = surface.transform(new Point2D.Double(image.getWidth(), image.getHeight()), new Point2D.Double());
        double tx = fitHorizontally(bounds, surfaceFocusArea, imageTopLeft, imageBottomRight);
        double ty = fitVertically(bounds, surfaceFocusArea, imageTopLeft, imageBottomRight);
        return translatedSurface(surface, tx, ty);
    }

    /**
     * Adjust image so that the focus area is completely inside the visible bounds and its center matches the golden
     * ratio.
     * 
     * @return AffineTransform
     */
    static AffineTransform matchFocusRegion(AffineTransform surface, Dimension image, Rectangle2D bounds, Rectangle2D imageFocusArea) {
        var surfaceFocusArea = surface.createTransformedShape(imageFocusArea).getBounds2D();
        Point2D imageTopLeft = surface.transform(new Point2D.Double(0.0, 0.0), new Point2D.Double());
        Point2D imageBottomRight = surface.transform(new Point2D.Double(image.getWidth(), image.getHeight()), new Point2D.Double());
        double tx = 0.0;
        double ty = 0.0;
        if (aspect(image) > aspect(bounds.getBounds().getSize())) {
            tx = fitHorizontally(bounds, surfaceFocusArea, imageTopLeft, imageBottomRight);
        } else {
            ty = fitVertically(bounds, surfaceFocusArea, imageTopLeft, imageBottomRight);
        }
        return translatedSurface(surface, tx, ty);
    }

    private static double fitHorizontally(Rectangle2D bounds, Rectangle2D surfaceFocusArea, Point2D imageTopLeft, Point2D imageBottomRight) {
        double tx = 0.0;
        if (surfaceFocusArea.getMinX() < bounds.getMinX() + bounds.getWidth() * goldenRatioFactorB) {
            if (surfaceFocusArea.getWidth() < bounds.getWidth()) {
                double optimal = bounds.getWidth() * goldenRatioFactorB - surfaceFocusArea.getCenterX();
                if (optimal > 0) {
                    double limit = bounds.getMinX() - imageTopLeft.getX();
                    tx = Math.min(optimal, limit);
                } else {
                    double limit = bounds.getMaxX() - imageBottomRight.getX();
                    tx = Math.max(optimal, limit);
                }
            } else {
                tx = -surfaceFocusArea.getCenterX() + bounds.getWidth() / 2;
            }
        } else if (surfaceFocusArea.getMaxX() > bounds.getMaxX() - bounds.getWidth() * goldenRatioFactorB) {
            if (surfaceFocusArea.getWidth() < bounds.getWidth()) {
                double optimal = bounds.getWidth() * goldenRatioFactorB - surfaceFocusArea.getCenterX();
                if (optimal > 0) {
                    double limit = bounds.getMaxX() - imageBottomRight.getX();
                    tx = -Math.min(-optimal, -limit);
                } else {
                    double limit = bounds.getMinX() - imageTopLeft.getX();
                    tx = -Math.min(-optimal, limit);
                }
            } else {
                tx = -surfaceFocusArea.getCenterX() + bounds.getWidth() / 2;
            }
        }
        return tx;
    }

    private static double fitVertically(Rectangle2D bounds, Rectangle2D surfaceFocusArea, Point2D imageTopLeft, Point2D imageBottomRight) {
        double ty = 0.0;
        if (surfaceFocusArea.getMinY() < bounds.getMinY() + bounds.getHeight() * goldenRatioFactorB) {
            if (surfaceFocusArea.getHeight() < bounds.getHeight()) {
                double optimal = bounds.getHeight() * goldenRatioFactorB - surfaceFocusArea.getCenterY();
                if (optimal > 0) {
                    double limit = bounds.getMinY() - imageTopLeft.getY();
                    ty = Math.min(optimal, limit);
                } else {
                    double limit = bounds.getMaxY() - imageBottomRight.getY();
                    ty = Math.max(optimal, limit);
                }
            } else {
                ty = -surfaceFocusArea.getCenterY() + bounds.getHeight() / 2;
            }
        } else if (surfaceFocusArea.getMaxY() > bounds.getMaxY() - bounds.getHeight() * goldenRatioFactorB) {
            if (surfaceFocusArea.getHeight() < bounds.getHeight()) {
                double optimal = bounds.getHeight() * goldenRatioFactorB - surfaceFocusArea.getCenterY();
                if (optimal > 0) {
                    double limit = bounds.getMaxY() - imageBottomRight.getY();
                    ty = -Math.min(-optimal, -limit);
                } else {
                    double limit = bounds.getMinY() - imageTopLeft.getY();
                    ty = -Math.min(-optimal, limit);
                }
            } else {
                ty = -surfaceFocusArea.getCenterY() + bounds.getHeight() / 2;
            }
        }
        return ty;
    }

    private static AffineTransform translatedSurface(AffineTransform surface, double tx, double ty) {
        if (tx != 0.0 || ty != 0.0) {
            var translate = AffineTransform.getTranslateInstance(tx, ty);
            translate.concatenate(surface);
            return translate;
        } else {
            return surface;
        }
    }

    static AffineTransform zoom(Rectangle2D focusArea, double zoom) {
        var zoomed = new AffineTransform();
        Point2D focusPoint = new Point2D.Double(focusArea.getCenterX(), focusArea.getCenterY());
        zoomed.translate(focusPoint.getX(), focusPoint.getY());
        zoomed.scale(zoom, zoom);
        zoomed.translate(-focusPoint.getX(), -focusPoint.getY());
        return zoomed;
    }

    /**
     * The scale factor to fill the bounding box
     * 
     * @param focusArea
     * 
     * @return A double value to scale the image onto the bounding box.
     */
    static double fitOutside(Dimension size, Optional<Rectangle2D> focusArea, Dimension onto) {
        return Math.max((double) onto.width / size.width, (double) onto.height / size.height);
    }

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

    private static final double MAXIMUM_INSET_ZOOM_FACTOR = 0.25;

    static double fitAspected(Dimension image, Dimension into) {
        var fit = fitInside(image, into);

        double width = image.width * fit;
        double horizontalInset = into.getWidth() - width;
        double height = image.height * fit;
        double verticalInset = into.getHeight() - height;

        double fillWidth = width + horizontalInset * MAXIMUM_INSET_ZOOM_FACTOR;
        double fillHeight = height + verticalInset * MAXIMUM_INSET_ZOOM_FACTOR;
        double reducedInset = Math.max(fillWidth / width, fillHeight / height);

        return fit * reducedInset;
    }

    /**
     * Return the aspect of the given size.
     * 
     * @param size
     * @return How much the dimension is wider than high.
     */
    static double aspect(Dimension size) {
        return (double) size.width / size.height;
    }

    static Dimension dimension(VolatileImage image) {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    static Dimension swap(Dimension dimension) {
        return new Dimension(dimension.height, dimension.width);
    }

    public static Point2D.Double scale(Point2D p, Dimension scale) {
        return new Point2D.Double(p.getX() * scale.width, p.getY() * scale.height);
    }

    static Rectangle2D.Double scale(Rectangle2D r, Dimension scale) {
        return new Rectangle2D.Double(r.getMinX() * scale.width, r.getMinY() * scale.height, r.getWidth() * scale.width,
                r.getHeight() * scale.height);
    }

    static Rectangle transform(AffineTransform t, double x, double y, double width, double height) {
        var lt = t.transform(new Point2D.Double(x, y), new Point2D.Double());
        var rb = t.transform(new Point2D.Double(width, height), new Point2D.Double());
        return new Rectangle((int) lt.getX(), (int) lt.getY(), (int) (rb.getX() - lt.getX()), (int) (rb.getY() - lt.getY()));
    }

}
