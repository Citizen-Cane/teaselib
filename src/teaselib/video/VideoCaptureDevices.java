package teaselib.video;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.video.VideoCaptureDeviceWebcamCapture;

public class VideoCaptureDevices {
    public static final DeviceCache<VideoCaptureDevice> Instance = new DeviceCache<VideoCaptureDevice>()
            .addFactory(VideoCaptureDeviceWebcamCapture.Factory);// .addFactory(VideoCaptureDeviceCV.Factory);
}
