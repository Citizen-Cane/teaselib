package teaselib.core.devices.video;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.videoInputLib.videoInput;
import org.junit.Test;

public class VideoCaptureVideoInputTests {

    @Test
    public void testVideoCapture() {
        videoInput.setComMultiThreaded(false);
        int n = videoInput.listDevices(false); // no debug output
        @SuppressWarnings("unused")
        List<String> deviceNames = new ArrayList<String>(n);
        int i = 0;
        String deviceName = videoInput.getDeviceName(i).getString();
        @SuppressWarnings("unused")
        int deviceId = videoInput.getDeviceIDFromName(deviceName);
        videoInput vi = new videoInput();
        vi.setupDevice(i);
        assertTrue(vi.isDeviceSetup(i));
        System.out.println(deviceName + " resolution = " + vi.getWidth(i) + ","
                + vi.getHeight(i));
        assertNotNull(vi.getPixels(0));
        vi.stopDevice(i);
        vi.close();
        return;
    }
}
