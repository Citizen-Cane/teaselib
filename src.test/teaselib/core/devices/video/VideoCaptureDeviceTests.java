/**
 * 
 */
package teaselib.core.devices.video;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import teaselib.core.devices.DeviceCache;
import teaselib.video.VideoCaptureDevices;

/**
 * @author someone
 *
 */
public class VideoCaptureDeviceTests {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Test
    public void testCaptureDeviceSorting() {
        final String deviceClassName = "TestDevicePath";
        final String webcamZ = DeviceCache.createDevicePath(deviceClassName,
                "Z-Industries YYYY Webcam");
        final String webcamRear = DeviceCache.createDevicePath(deviceClassName,
                "Microsoft Surface Rear HD Camera");
        final String dev2 = DeviceCache.createDevicePath(deviceClassName,
                "Microsoft Surface Front HD Camera");
        final String dev3 = DeviceCache.createDevicePath(deviceClassName,
                "A-Labs XXX Usb Camera");
        List<String> devicePaths = Arrays.asList(webcamZ, webcamRear, dev2, dev3);
        VideoCaptureDevices.sort(devicePaths);
        assertEquals(dev3, devicePaths.get(0));
        assertEquals(webcamZ, devicePaths.get(1));
        assertEquals(dev2, devicePaths.get(2));
        assertEquals(webcamRear, devicePaths.get(3));
    }

}
