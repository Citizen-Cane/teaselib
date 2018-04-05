package teaselib.core.devices.video;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.videoInputLib.videoInput;
import org.junit.Test;

public class VideoCaptureVideoInputTests {

    @Test
    public void testVideoCapture() {
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
}
