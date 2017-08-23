package teaselib.video;

import java.util.Iterator;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.Configuration;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.devices.video.VideoCaptureDeviceCV;

public abstract class VideoCaptureDevice implements Iterable<Mat>, Device.Creatable {
    private static DeviceCache<VideoCaptureDevice> Instance;

    public static synchronized DeviceCache<VideoCaptureDevice> getDeviceCache(Devices devices,
            Configuration configuration) {
        if (Instance == null) {
            Instance = new DeviceCache<VideoCaptureDevice>()
                    .addFactory(VideoCaptureDeviceCV.getDeviceFactory(devices, configuration));
            // .addFactory(VideoCaptureDeviceVideoInput.Factory);
        }
        return Instance;
    }

    public static final Size DefaultResolution = new Size(0, 0);

    public static final double DefaultFPS = 0.0;

    public abstract void open();

    public abstract void fps(double fps);

    public abstract double fps();

    public abstract ResolutionList getResolutions();

    public abstract void resolution(Size size);

    public abstract Size resolution();

    @Override
    public abstract Iterator<Mat> iterator();

    @Override
    public abstract void close();
}