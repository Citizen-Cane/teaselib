package teaselib.hosts;

import static org.junit.Assert.*;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class TransformTest {

    @Test
    public void testTransformToSurfaceBitmap() {
        Dimension image = new Dimension(320, 240);
        Rectangle bounds = new Rectangle(16, 16, 320, 240);
        var t = Transform.maxImage(image, bounds.getSize(), Optional.empty());

        assertEquals(new Point2D.Double(0.0, 0.0), t.transform(new Point2D.Double(0.0, 0.0), new Point2D.Double()));
        assertEquals(new Point2D.Double(320.0, 240.0),
                t.transform(new Point2D.Double(320.0, 240.0), new Point2D.Double()));
    }

}
