package teaselib.video;

import java.util.ArrayList;
import java.util.List;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.video.VideoCaptureDeviceCV;

public class VideoCaptureDevices extends DeviceCache<VideoCaptureDevice> {
    public static final DeviceCache<VideoCaptureDevice> Instance = new VideoCaptureDevices(
            VideoCaptureDeviceCV.DeviceClassName,
            new DeviceCache.DeviceFactory<VideoCaptureDevice>() {

                @Override
                public List<String> getDevices() {
                    List<String> deviceNames = new ArrayList<>();
                    deviceNames.addAll(VideoCaptureDeviceCV.getDevicesPaths());
                    return deviceNames;
                }

                @Override
                public VideoCaptureDevice getDevice(String devicePath) {
                    return VideoCaptureDeviceCV
                            .get(DeviceCache.getDeviceName(devicePath));
                }
            });

    public VideoCaptureDevices(String deviceClassName,
            teaselib.core.devices.DeviceCache.DeviceFactory<VideoCaptureDevice> factory) {
        super(deviceClassName, factory);
    }

    @Override
    public VideoCaptureDevice getDefaultDevice() {
        // Get the front camera
        String defaultId = getLast(getDevices());
        return getDevice(defaultId);
    }

}
