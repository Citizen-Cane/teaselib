package teaselib.core.devices.video;

import static org.junit.Assert.*;

import org.bytedeco.javacpp.opencv_core.Size;
import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.Devices;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;

public class VideoDeviceResolutionTests {
    ResolutionList resolutions = new ResolutionList(new Size(1920, 1080), new Size(1280, 960), new Size(640, 480));

    @Test
    public void testFindingSimilarResolution() {
        try (Size desired = new Size(1920, 1080);) {
            assertEquals(resolutions.get(0), resolutions.getMatchingOrSimilar(desired));
        }
        try (Size desired = new Size(800, 600);) {
            assertEquals(resolutions.get(2), resolutions.getMatchingOrSimilar(desired));
        }
    }

    @Test
    public void testFindingMatchingResolution() {
        assertTrue(resolutions.contains(resolutions.get(0)));
        try (Size resolution = new Size(1920, 1080);) {
            assertTrue(resolutions.contains(resolution));
        }
        assertTrue(resolutions.contains(resolutions.get(1)));
        try (Size resolution = new Size(1280, 960);) {
            assertTrue(resolutions.contains(resolution));
        }
        assertTrue(resolutions.contains(resolutions.get(2)));
        try (Size resolution = new Size(640, 480);) {
            assertTrue(resolutions.contains(resolution));
        }
        try (Size resolution = new Size(0, 0);) {
            assertFalse(resolutions.contains(resolution));
        }
        try (Size resolution = new Size(1280, 1080);) {
            assertFalse(resolutions.contains(resolution));
        }
        try (Size resolution = new Size(640, 960);) {
            assertFalse(resolutions.contains(resolution));
        }
    }

    @Test
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
