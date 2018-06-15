package teaselib.core.javacv.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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
            Mat mat1 = ring.current();
            assertNotNull(mat1);
            Mat mat3 = ring.last();
            assertNotNull(mat3);

            assertEquals(200, mat1.cols());
            assertEquals(100, mat1.rows());
            assertEquals(200, mat3.cols());
            assertEquals(100, mat3.rows());

            ring.advance();
            assertEquals(mat3, ring.current());
            assertEquals(mat1, ring.previous());
            assertNotEquals(mat1, ring.last());

            ring.advance();
            assertNotEquals(mat1, ring.current());
            assertNotEquals(mat3, ring.current());
            assertEquals(mat1, ring.last());
            assertEquals(mat3, ring.previous());

            ring.advance();
            assertEquals(mat1, ring.current());
            assertEquals(mat3, ring.last());
            assertNotEquals(mat1, ring.previous());
            assertNotEquals(mat3, ring.previous());

            video.create(200, 400, opencv_core.CV_8UC1);
            ring.resize(video::clone, 3);
            for (int i = 0; i < ring.size(); i++) {
                assertEquals(400, ring.current().cols());
                assertEquals(200, ring.current().rows());
            }
        }
    }
}
