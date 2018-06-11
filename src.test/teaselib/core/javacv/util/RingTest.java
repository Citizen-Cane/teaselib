package teaselib.core.javacv.util;

import static org.junit.Assert.*;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.junit.Test;

public class RingTest {
    @Test
    public void testRing() {
        try (Mat video = new Mat();) {
            video.create(100, 200, opencv_core.CV_8UC1);
            assertEquals(200, video.cols());
            assertEquals(100, video.rows());
            assertEquals(opencv_core.CV_8UC1, video.type());

            Ring<Mat> ring = new Ring<>(video::clone, 3);
            Mat mat1 = ring.getCurrent();
            assertNotNull(mat1);
            Mat mat3 = ring.getLast();
            assertNotNull(mat3);

            assertEquals(200, mat1.cols());
            assertEquals(100, mat1.rows());
            assertEquals(200, mat3.cols());
            assertEquals(100, mat3.rows());

            ring.advance();
            assertEquals(mat3, ring.getCurrent());
            assertNotEquals(mat1, ring.getLast());

            ring.advance();
            assertNotEquals(mat1, ring.getCurrent());
            assertNotEquals(mat3, ring.getCurrent());
            assertEquals(mat1, ring.getLast());

            ring.advance();
            assertEquals(mat1, ring.getCurrent());
            assertEquals(mat3, ring.getLast());

            video.create(200, 400, opencv_core.CV_8UC1);
            ring.resize(3);
            assertEquals(400, ring.getCurrent().cols());
            assertEquals(200, ring.getCurrent().rows());
        }
    }
}
