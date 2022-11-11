package teaselib.host;

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
        Assert.assertEquals(message, expected.getX(), actual.getX(), 10E-3);
        Assert.assertEquals(message, expected.getY(), actual.getY(), 10E-3);
    }

    private static Point2D.Double p(double x, double y) {
        return new Point2D.Double(x, y);
    }

    @Test
    public void testTransformToSurfaceBitmapInstructionalImage() {
        Dimension image = new Dimension(320, 240);
        Rectangle2D bounds = new Rectangle(16, 16, 320, 240);

        var t = Transform.maxImage(image, bounds, Optional.empty(), Transform::fitOutside);
        assertEquals(t, p(0.0, 0.0), p(0.0, 0.0));
        assertEquals(t, p(320.0, 240.0), p(320.0, 240.0));
    }

    @Test
    public void testTransformToSurfaceBitmapIdentity() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(16, 16, 320, 240);

        var t = Transform.maxImage(image, bounds, Optional.of(head), Transform::fitOutside);
        assertEquals(t, p(0.0, 0.0), p(0.0, 0.0));
        assertEquals(t, p(320.0, 240.0), p(320.0, 240.0));
    }

    @Test
    public void testTransformToSurfaceBitmapLandscapeLandscape() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(16, 16, 320, 120);

        var t = Transform.maxImage(image, bounds, Optional.of(head), Transform::fitOutside);
        assertEquals(t, p(0.0, -60.0), p(0.0, 0.0));
        assertEquals(t, p(320.0, 180.0), p(320.0, 240.0));
    }

    @Test
    public void testTransformToSurfaceBitmapLandscapePortrait() {
        Dimension image = new Dimension(240, 320);
        Rectangle bounds = new Rectangle(32, 32, 320, 120);

        var t = Transform.maxImage(image, bounds, Optional.of(head), Transform::fitOutside);
        assertEquals(t, p(0.0, -153.333), p(0.0, 0.0));
        assertEquals(t, p(320.0, 273.333), p(240.0, 320.0));
    }

    @Test
    public void testVisibleFocusLandscapePortraitTop() {
        Dimension image = new Dimension(240, 320);
        Rectangle bounds = new Rectangle(0, 0, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(top), Transform::fitOutside);
        Rectangle2D.Double imageFocusArea = Transform.scale(top, image);

        Point2D focusTop = t.transform(new Point2D.Double(imageFocusArea.getCenterX(), imageFocusArea.getMinY()),
                new Point2D.Double());
        assertTrue(focusTop.getY() < 0.0);

        t = Transform.matchGoldenRatioOrKeepVisible(t, image, bounds, imageFocusArea);
        assertEquals(t, p(0.0, -7.497), p(0.0, 0.0)); //
        assertEquals(t, p(320.0, 419.169), p(240.0, 320.0));
        assertEquals(t, p(160.0, -7.497), new Point2D.Double(imageFocusArea.getCenterX(), imageFocusArea.getMinY()));
        assertEquals(t, p(160.0, 99.169), new Point2D.Double(imageFocusArea.getCenterX(), imageFocusArea.getMaxY()));
        // upper bounds transformed to y == 0
    }

    @Test
    public void testVisibleFocusLandscapePortraitBottom() {
        Dimension image = new Dimension(240, 320);
        Rectangle bounds = new Rectangle(0, 0, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(bottom), Transform::fitOutside);
        Rectangle2D.Double imageFocusArea = Transform.scale(bottom, image);

        Point2D focusBottom = t.transform(new Point2D.Double(imageFocusArea.getCenterX(), imageFocusArea.getMaxY()),
                new Point2D.Double());
        assertTrue(focusBottom.getY() > bounds.height);

        t = Transform.matchGoldenRatioOrKeepVisible(t, image, bounds, imageFocusArea);
        assertEquals(t, p(0.0, -306.666), p(0.0, 0.0));
        assertEquals(t, p(320.0, 120.0), p(240.0, 320.0));
        assertEquals(t, p(160.0, 13.333), new Point2D.Double(imageFocusArea.getCenterX(), imageFocusArea.getMinY()));
        assertEquals(t, p(160.0, 120.0), new Point2D.Double(imageFocusArea.getCenterX(), imageFocusArea.getMaxY()));
    }

    @Test
    public void testVisibleFocusLandscapePortraitLeftScaledToBorder() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(32, 32, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(left), Transform::fitOutside);
        Rectangle2D.Double imageFocusArea = Transform.scale(left, image);
        assertEquals(t, new Point2D.Double(0.0, 60.0),
                new Point2D.Double(imageFocusArea.getMinX(), imageFocusArea.getCenterY()));
        assertEquals(t, new Point2D.Double(bounds.width * left.width, 60.0),
                new Point2D.Double(imageFocusArea.getMaxX(), imageFocusArea.getCenterY()));
        // Portrait image already scaled to horizontal bounds<
    }

    @Test
    public void testVisibleFocusLandscapePortraitRightScaledToBorder() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(32, 32, 320, 120);
        var t = Transform.maxImage(image, bounds, Optional.of(right), Transform::fitOutside);
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
        var t = Transform.maxImage(image, bounds, Optional.of(head), Transform::fitOutside);
        Rectangle2D.Double focusAreaImage = Transform.scale(head, image);

        Point2D focusTop = t.transform(new Point2D.Double(focusAreaImage.getCenterX(), focusAreaImage.getMinY()),
                new Point2D.Double());
        assertTrue(focusTop.getY() < 0.0);

        t = Transform.matchGoldenRatioOrKeepVisible(t, image, bounds, focusAreaImage);
        assertEquals(t, p(2.229, -114.164), p(0.0, 0.0)); //
        assertEquals(t, p(322.229, 312.502), p(240.0, 320.0));
        assertEquals(t, p(82.229, -7.497), new Point2D.Double(focusAreaImage.getMinX(), focusAreaImage.getMinY()));
        assertEquals(t, p(162.229, 99.169), new Point2D.Double(focusAreaImage.getMaxX(), focusAreaImage.getMaxY()));
    }

}
