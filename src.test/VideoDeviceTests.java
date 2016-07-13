import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bytedeco.javacpp.opencv_core.Size;
import org.junit.Test;

import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;
import teaselib.video.VideoCaptureDevices;

public class VideoDeviceTests {
    ResolutionList resolutions = new ResolutionList(new Size(1920, 1080),
            new Size(1280, 960), new Size(640, 480));

    @SuppressWarnings("resource")
    @Test
    public void testFindingSimilarResolution() {
        Size resolution = resolutions
                .getMatchingOrSimilar(new Size(1920, 1080));
        assertEquals(resolution, resolutions.get(0));
        assertEquals(resolutions.get(2),
                resolutions.getMatchingOrSimilar(new Size(800, 600)));
    }

    @Test
    public void testFindingMatchingResolution() {
        assertTrue(resolutions.contains(resolutions.get(0)));
        assertTrue(resolutions.contains(new Size(1920, 1080)));
        assertTrue(resolutions.contains(resolutions.get(1)));
        assertTrue(resolutions.contains(new Size(1280, 960)));
        assertTrue(resolutions.contains(resolutions.get(2)));
        assertTrue(resolutions.contains(new Size(640, 480)));

        assertFalse(resolutions.contains(new Size(0, 0)));
        assertFalse(resolutions.contains(new Size(1280, 1080)));
        assertFalse(resolutions.contains(new Size(640, 960)));
    }

    //@Test
    @SuppressWarnings("resource")
    public void testGetResolutionBeforeDeviceOpen() {
        VideoCaptureDevice defaultDevice = VideoCaptureDevices.Instance
                .getDefaultDevice();
        Size resolution = defaultDevice.captureSize();
        assertNotNull(resolution);
        defaultDevice.open(resolution);
        Size resolution2 = defaultDevice.captureSize();
        assertNotNull(resolution2);
    }
}
