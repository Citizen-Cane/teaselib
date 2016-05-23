package teaselib.video;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.video.VideoCaptureDeviceCV;

public class VideoCaptureDevices {
    public static final DeviceCache<VideoCaptureDevice> Instance = new DeviceCache<VideoCaptureDevice>(
            VideoCaptureDeviceCV.DeviceClassName,
            new DeviceCache.DeviceFactory<VideoCaptureDevice>() {
                @Override
                public List<String> getDevices() {
                    List<String> deviceNames = new ArrayList<String>();
                    deviceNames.addAll(VideoCaptureDeviceCV.getDevicesPaths());
                    return deviceNames;
                }

                @Override
                public VideoCaptureDevice getDevice(String devicePath) {
                    return VideoCaptureDeviceCV
                            .get(DeviceCache.getDeviceName(devicePath));
                }
            });
}
