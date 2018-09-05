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
    public void testVideoCaptureOpeClose() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        DeviceCache.connect(vc);
        assertFalse(vc.active());

        for (int i = 0; i < 2; i++)
            try (Size vga = new Size(640, 480); Size hd = new Size(1280, 720);) {
                try {
                    assertEquals(VideoCaptureDevice.DefaultResolution.width(), vc.resolution().width());
                    assertEquals(VideoCaptureDevice.DefaultResolution.height(), vc.resolution().height());
                } catch (IllegalStateException e) {
                    // Correct
                }

                vc.open();
                vc.resolution(hd);
                assertEquals(hd.width(), vc.resolution().width());
                assertEquals(hd.height(), vc.resolution().height());

                Iterator<Mat> video = vc.iterator();
                Mat image = video.next();
                assertEquals(hd.width(), image.cols());
                assertEquals(hd.height(), image.rows());

                vc.resolution(vga);
                assertEquals(vga.width(), vc.resolution().width());
                assertEquals(vga.height(), vc.resolution().height());

                image = video.next();
                assertEquals(hd.width(), image.cols());
                assertEquals(hd.height(), image.rows());

                vc.close();
            }
    }

    @Test
    public void testVideoCaptureResolutionSwitch() {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        VideoCaptureDevice vc = devices.get(VideoCaptureDevice.class).getDefaultDevice();
        DeviceCache.connect(vc);
        assertFalse(vc.active());

        try (Size vga = new Size(640, 480); Size hd = new Size(1280, 720);) {
            try {
                vc.resolution(hd);
                assertEquals(VideoCaptureDevice.DefaultResolution.width(), vc.resolution().width());
                assertEquals(VideoCaptureDevice.DefaultResolution.width(), vc.resolution().height());
            } catch (IllegalStateException e) {
                // Correct
            }

            vc.open();
            vc.resolution(hd);
            assertEquals(hd.width(), vc.resolution().width());
            assertEquals(hd.height(), vc.resolution().height());

            Iterator<Mat> video = vc.iterator();
            Mat image = video.next();
            assertEquals(hd.width(), image.cols());
            assertEquals(hd.height(), image.rows());

            vc.resolution(vga);
            assertEquals(vga.width(), vc.resolution().width());
            assertEquals(vga.height(), vc.resolution().height());

            image = video.next();
            assertEquals(hd.width(), image.cols());
            assertEquals(hd.height(), image.rows());

            vc.close();
        }
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
