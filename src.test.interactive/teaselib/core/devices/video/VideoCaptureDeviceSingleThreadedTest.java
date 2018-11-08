package teaselib.core.devices.video;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_highgui;
import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceSingleThreadedTest {
    @Test
    public void testVideoCaptureOpenClose() {
        VideoCaptureDevice vc = getDevice();

        for (int i = 0; i < 2; i++) {
            try (Size vga = new Size(640, 480); Size hd = new Size(1280, 720);) {
                testResolutionSwitch(vc, vga, hd);
            }
        }
    }

    @Test
    public void testVideoCaptureResolutionSwitch() {
        VideoCaptureDevice vc = getDevice();

        try (Size vga = new Size(640, 480); Size hd = new Size(1280, 720);) {
            testResolutionSwitch(vc, vga, hd);
        }
    }

    private static VideoCaptureDevice getDevice() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        DeviceCache.connect(vc);
        assertFalse(vc.active());
        assertEquals(VideoCaptureDevice.DefaultResolution.width(), vc.resolution().width());
        assertEquals(VideoCaptureDevice.DefaultResolution.height(), vc.resolution().height());
        return vc;
    }

    private static void testResolutionSwitch(VideoCaptureDevice vc, Size vga, Size hd) {
        try {
            vc.resolution(hd);
            fail();
        } catch (IllegalStateException e) {
            // Correct
        }

        vc.open();
        assertTrue(vc.resolution().width() > 0);
        assertTrue(vc.resolution().height() > 0);
        Iterator<Mat> video = vc.iterator();

        vc.resolution(hd);
        try (Mat image = video.next();) {
            assertEquals(hd.width(), image.cols());
            assertEquals(hd.height(), image.rows());

            assertEquals(hd.width(), vc.resolution().width());
            assertEquals(hd.height(), vc.resolution().height());
        }

        vc.resolution(vga);
        try (Mat image = video.next();) {
            assertEquals(hd.width(), image.cols());
            assertEquals(hd.height(), image.rows());

            assertEquals(vga.width(), vc.resolution().width());
            assertEquals(vga.height(), vc.resolution().height());
        }

        vc.close();
    }

    @Test
    public void testVideoCapture() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        DeviceCache.connect(vc);
        assertFalse(vc.active());

        capture(vc);

        // Close device to avoid crash on system exit
        vc.close();
    }

    private static void capture(VideoCaptureDevice vc) {
        try (Size size = new Size(640, 480);) {
            vc.open();
            vc.resolution(size);
            vc.fps(30);
            assertEquals(size.width(), vc.resolution().width());
            assertEquals(size.height(), vc.resolution().height());
            for (Mat mat : vc) {
                assertEquals(size.width(), mat.cols());
                assertEquals(size.height(), mat.rows());
                opencv_highgui.imshow("Test", mat);
                if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
                    break;
                }
            }
        }
    }
}
