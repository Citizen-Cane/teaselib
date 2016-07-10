package teaselib.video;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.video.VideoCaptureDeviceCV;

public class VideoCaptureDevices {
    public static final DeviceCache<VideoCaptureDevice> Instance = new DeviceCache<VideoCaptureDevice>()
            .addFactory(VideoCaptureDeviceCV.Factory);
}
