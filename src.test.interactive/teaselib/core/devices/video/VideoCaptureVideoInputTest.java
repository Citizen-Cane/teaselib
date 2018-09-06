package teaselib.core.devices.video;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.videoInputLib.videoInput;
import org.junit.Test;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureVideoInputTest {

    @Test
    public void testDevices() {
        videoInput.setComMultiThreaded(false);

        int n = videoInput.listDevices(false); // no debug output
        List<String> deviceNames = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            deviceNames.add(videoInput.getDeviceName(i).getString());
        }

        for (String deviceName : deviceNames) {
            int i = 0;
            try (videoInput vi = new videoInput();) {
                vi.setupDevice(i);
                assertTrue(vi.isDeviceSetup(i));
                int deviceId = videoInput.getDeviceIDFromName(deviceName);
                System.out.println("Device " + deviceId + " " + deviceName + " resolution = " + vi.getWidth(i) + ","
                        + vi.getHeight(i));
                assertNotNull(vi.getPixels(0));
                vi.stopDevice(i);
            }
        }
    }

    @Test
    public void testVideoCapture() throws InterruptedException {
        Configuration config = DebugSetup.getConfiguration();
        Devices devices = new Devices(config);
        DeviceFactory<VideoCaptureDeviceVideoInput> deviceFactory = VideoCaptureDeviceVideoInput
                .getDeviceFactory(devices, config);
        Map<String, VideoCaptureDeviceVideoInput> deviceCache = new HashMap<>();
        List<String> devicePaths = deviceFactory.enumerateDevicePaths(deviceCache);
        Optional<String> devicePath = devicePaths.stream().findFirst();
        assertTrue(devicePath.isPresent());
        VideoCaptureDevice vc = deviceFactory.getDevice(devicePath.get());
        DeviceCache.connect(vc);
        assertFalse(vc.active());

        capture(vc);

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
