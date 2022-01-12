package teaselib.hosts;

import static org.junit.Assert.*;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TransformTest {

    static final Rectangle2D.Double top = new Rectangle2D.Double(0.25, 0.0, 0.5, 0.25);
    static final Rectangle2D.Double bottom = new Rectangle2D.Double(0.25, 0.75, 0.5, 0.25);
    static final Rectangle2D.Double left = new Rectangle2D.Double(0.0, 0.25, 0.25, 0.5);
    static final Rectangle2D.Double right = new Rectangle2D.Double(0.75, 0.25, 0.25, 0.5);

    static final Rectangle2D.Double head = new Rectangle2D.Double(0.25, 0.25, 0.25, 0.25);

    private static void assertEquals(AffineTransform t, Point2D expected, Point2D input) {
        Point2D actual = t.transform(input, new Point2D.Double());
        assertEquals(expected, actual);
    }

    private static void assertEquals(Point2D expected, Point2D actual) {
        String message = "Expected " + expected + " but got " + actual;
        Assert.assertEquals(message, expected.getX(), actual.getX(), 10E-8);
        Assert.assertEquals(message, expected.getY(), actual.getY(), 10E-8);
    }

    private static Point2D.Double p(double x, double y) {
        return new Point2D.Double(x, y);
    }

    @Test
    public void testTransformToSurfaceBitmapInstructionalImage() {
        Dimension image = new Dimension(320, 240);
        Rectangle2D bounds = new Rectangle(16, 16, 320, 240);

        var t = Transform.maxImage(image, bounds, Optional.empty());
        assertEquals(t, p(0.0, 0.0), p(0.0, 0.0));
        assertEquals(t, p(320.0, 240.0), p(320.0, 240.0));
    }

    @Test
    public void testTransformToSurfaceBitmapIdentity() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(16, 16, 320, 240);

        var t = Transform.maxImage(image, bounds, Optional.of(head));
        assertEquals(t, p(0.0, 0.0), p(0.0, 0.0));
        assertEquals(t, p(320.0, 240.0), p(320.0, 240.0));
    }

    @Test
    public void testTransformToSurfaceBitmapLandscapeLandscape() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(16, 16, 320, 120);

        var t = Transform.maxImage(image, bounds, Optional.of(head));
        assertEquals(t, p(0.0, -60.0), p(0.0, 0.0));
        assertEquals(t, p(320.0, 180.0), p(320.0, 240.0));
    }

    @Test
    public void testTransformToSurfaceBitmapLandscapePortrait() {
        Dimension image = new Dimension(240, 320);
        Rectangle bounds = new Rectangle(32, 32, 320, 120);

        var t = Transform.maxImage(image, bounds, Optional.of(head));
        assertEquals(t, p(40.0, -100.0), p(0.0, 0.0));
        assertEquals(t, p(280.0, 220.0), p(240.0, 320.0));
    }

    @Test
    public void testVisibleFocusLandscapePortraitTop() {
        Dimension image = new Dimension(240, 320);
        Rectangle bounds = new Rectangle(0, 0, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(top));
        Rectangle2D.Double focusAreaImage = Transform.scale(top, image);

        Point2D focusTop = t.transform(new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMinY()),
                new Point2D.Double());
        assertTrue(focusTop.getY() < 0.0);

        t = Transform.keepFocusAreaVisible(t, image, bounds, focusAreaImage);
        assertEquals(t, p(40.0, 0.0), p(0.0, 0.0)); //
        assertEquals(t, p(280.0, 320.0), p(240.0, 320.0));
        assertEquals(t, p(160.0, 0.0), new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMinY()));
        assertEquals(t, p(160.0, 80.0), new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMaxY()));
        // upper bounds transformed to y == 0
    }

    @Test
    public void testVisibleFocusLandscapePortraitBottom() {
        Dimension image = new Dimension(240, 320);
        Rectangle bounds = new Rectangle(0, 0, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(bottom));
        Rectangle2D.Double focusAreaImage = Transform.scale(bottom, image);

        Point2D focusBottom = t.transform(new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMaxY()),
                new Point2D.Double());
        assertTrue(focusBottom.getY() > bounds.height);

        t = Transform.keepFocusAreaVisible(t, image, bounds, focusAreaImage);
        assertEquals(t, p(40.0, -200.0), p(0.0, 0.0)); //
        assertEquals(t, p(280.0, 120.0), p(240.0, 320.0));
        assertEquals(t, p(160.0, 40.0), new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMinY()));
        assertEquals(t, p(160.0, 120.0), new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMaxY()));
        // lower bounds transformed to y == bounds.height
    }

    @Test
    public void testVisibleFocusLandscapePortraitLeftScaledToBorder() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(32, 32, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(left));
        Rectangle2D.Double focusAreaImage = Transform.scale(left, image);
        assertEquals(t, new Point2D.Double(0.0, 60.0),
                new Point2D.Double(focusAreaImage.getMinX(), focusAreaImage.getCenterY()));
        assertEquals(t, new Point2D.Double(bounds.width * left.width, 60.0),
                new Point2D.Double(focusAreaImage.getMaxX(), focusAreaImage.getCenterY()));
        // Portrait image already scaled to horizontal bounds<
    }

    @Test
    public void testVisibleFocusLandscapePortraitRightScaledToBorder() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(32, 32, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(right));
        Rectangle2D.Double focusAreaImage = Transform.scale(right, image);

        assertEquals(t, new Point2D.Double(bounds.width * (1.0 - left.width), 60.0),
                new Point2D.Double(focusAreaImage.getMinX(), focusAreaImage.getCenterY()));
        assertEquals(t, new Point2D.Double(bounds.width, 60.0),
                new Point2D.Double(focusAreaImage.getMaxX(), focusAreaImage.getCenterY()));
        // Portrait image already scaled to horizontal bounds
    }

    @Test
    public void testVisibleFocusLandscapePortraitHead() {
        Dimension image = new Dimension(240, 320);
        Rectangle bounds = new Rectangle(32, 32, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(head));
        Rectangle2D.Double focusAreaImage = Transform.scale(head, image);

        Point2D focusTop = t.transform(new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMinY()),
                new Point2D.Double());
        assertTrue(focusTop.getY() < 0.0);

        t = Transform.keepFocusAreaVisible(t, image, bounds, focusAreaImage);
        assertEquals(t, p(40.0, -74.1640786498), p(0.0, 0.0)); //
        assertEquals(t, p(280.0, 245.83592135011997), p(240.0, 320.0));
        assertEquals(t, p(100.0, 5.835921350119989),
                new Point2D.Double(focusAreaImage.getMinX(), focusAreaImage.getMinY()));
        assertEquals(t, p(160.0, 85.83592135011999),
                new Point2D.Double(focusAreaImage.getMaxX(), focusAreaImage.getMaxY()));
    }

}
