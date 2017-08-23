package teaselib.video;

import java.util.Comparator;
import java.util.List;

import teaselib.core.devices.DeviceCache;

public abstract class VideoCaptureDevices {
    private static final String Front = "front";
    private static final String Rear = "rear";

    /**
     * Sort device names so that external video capture devices appear first in the list, and are thus chosen as the
     * default device. Front cameras are sorted next, because we'll usually need to capture and see the screen
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
                if (deviceNameA.contains(Front) && deviceNameB.contains(Rear)) {
                    return -1;
                } else if (deviceNameA.contains(Rear) && deviceNameB.contains(Front)) {
                    return 1;
                } else if (deviceNameA.contains(Front) && !deviceNameB.contains(Front)) {
                    return 1;
                } else if (deviceNameA.contains(Rear) && !deviceNameB.contains(Rear)) {
                    return 1;
                } else {
                    return a.compareTo(b);
                }
            }
        });
        return devicePaths;
    }
}
