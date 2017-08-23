package teaselib.core.devices.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bytedeco.javacpp.opencv_core.Size;
import org.junit.Test;

import teaselib.core.Configuration;
import teaselib.core.devices.Devices;
import teaselib.test.DebugSetup;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;

public class VideoDeviceResolutionTests {
    ResolutionList resolutions = new ResolutionList(new Size(1920, 1080), new Size(1280, 960), new Size(640, 480));

    @SuppressWarnings("resource")
    @Test
    public void testFindingSimilarResolution() {
        Size resolution = resolutions.getMatchingOrSimilar(new Size(1920, 1080));
        assertEquals(resolution, resolutions.get(0));
        assertEquals(resolutions.get(2), resolutions.getMatchingOrSimilar(new Size(800, 600)));
    }

    @SuppressWarnings("resource")
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

    // @Test
    @SuppressWarnings("resource")
    public void testGetResolutionBeforeDeviceOpen() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);

        VideoCaptureDevice defaultDevice = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        defaultDevice.open();
        Size resolution = defaultDevice.resolution();
        assertNotNull(resolution);
        defaultDevice.resolution(resolution);
        Size resolution2 = defaultDevice.resolution();
        assertNotNull(resolution2);
    }

    @Test
    @SuppressWarnings("resource")
    public void testSmallestFit() {
        Size bestFit1 = ResolutionList.getSmallestFit(new Size(1920, 1080), new Size(320, 240));
        assertEquals(320, bestFit1.width());
        assertEquals(180, bestFit1.height());

        Size bestFit2 = ResolutionList.getSmallestFit(new Size(1920, 1080), new Size(350, 260));
        assertEquals(384, bestFit2.width());
        assertEquals(216, bestFit2.height());

        Size bestFit3 = ResolutionList.getSmallestFit(new Size(1920, 1080), new Size(310, 230));
        assertEquals(320, bestFit3.width());
        assertEquals(180, bestFit3.height());
    }
}
