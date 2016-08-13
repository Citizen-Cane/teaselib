package teaselib.video;

import java.util.Comparator;
import java.util.List;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.video.VideoCaptureDeviceCV;

public class VideoCaptureDevices {
    public static final DeviceCache<VideoCaptureDevice> Instance = new DeviceCache<VideoCaptureDevice>()
            .addFactory(VideoCaptureDeviceCV.Factory);
    // .addFactory(VideoCaptureDeviceVideoInput.Factory);
    // .addFactory(VideoCaptureDeviceWebcamCapture.Factory);

    static final String front = "front";
    static final String rear = "rear";

    /**
     * Sort device names so that external video capture devices appear first in
     * the list, and are thus chosen as the default device. Front cameras are
     * sorted next, because we'll usually need to capture and see the screen
     * simultaneously.
     * 
     * @param devicePaths
     *            The list of device paths to be sorted.
     * @return The sorted list.
     */
    public static List<String> sort(List<String> devicePaths) {
        devicePaths.sort(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                String deviceNameA = DeviceCache.getDeviceName(a).toLowerCase();
                String deviceNameB = DeviceCache.getDeviceName(b).toLowerCase();
                if (deviceNameA.contains(front) && deviceNameB.contains(rear)) {
                    return -1;
                } else if (deviceNameA.contains(rear)
                        && deviceNameB.contains(front)) {
                    return 1;
                } else if (deviceNameA.contains(front)
                        && !deviceNameB.contains(front)) {
                    return 1;
                } else if (deviceNameA.contains(rear)
                        && !deviceNameB.contains(rear)) {
                    return 1;
                } else {
                    return a.compareTo(b);
                }
            }
        });
        return devicePaths;
    }
}
