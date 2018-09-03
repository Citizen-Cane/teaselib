package teaselib.core.devices.video;

import static org.bytedeco.javacpp.opencv_videoio.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.bridj.Platform;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;
import teaselib.video.VideoCaptureDevices;

// TODO upgrade to source level 1.8 -> change back to interface

public class VideoCaptureDeviceCV extends VideoCaptureDevice /* extends WiredDevice */ {
    private static final Logger logger = LoggerFactory.getLogger(VideoCaptureDeviceCV.class);

    private static final String DeviceClassName = "JavaCVVideoCapture";

    private static final boolean UseVideoInput = Platform.isWindows();

    private static final class MyDeviceFactory extends DeviceFactory<VideoCaptureDeviceCV> {
        private MyDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
            super(deviceClass, devices, configuration);
        }

        @Override
        public List<String> enumerateDevicePaths(Map<String, VideoCaptureDeviceCV> deviceCache) {
            final List<String> devicePaths;
            if (UseVideoInput) {
                List<String> deviceNames = VideoCaptureDeviceVideoInput.enumerateVideoInputDevices();
                VideoCaptureDevices.sort(deviceNames);
                devicePaths = new ArrayList<>(deviceNames.size());
                for (String deviceName : deviceNames) {
                    devicePaths.add(DeviceCache.createDevicePath(DeviceClassName, deviceName));
                }
            } else {
                int i = 0;
                List<VideoCaptureDeviceCV> captureDevices = getCaptureDevices(deviceCache);
                devicePaths = new ArrayList<>(captureDevices.size());
                for (VideoCaptureDeviceCV device : captureDevices) {
                    String devicePath = DeviceCache.createDevicePath(VideoCaptureDeviceCV.DeviceClassName,
                            Integer.toString(i++));
                    deviceCache.put(device.getDevicePath(), device);
                    devicePaths.add(devicePath);
                }
            }
            return devicePaths;
        }

        private List<VideoCaptureDeviceCV> getCaptureDevices(Map<String, VideoCaptureDeviceCV> deviceCache) {
            int i = 0;
            List<VideoCaptureDeviceCV> devices = new ArrayList<>();
            while (true) {
                // Only add new devices, because we want them to be singletons
                if (i >= deviceCache.size())
                    // Detect new devices
                    try {
                        VideoCaptureDeviceCV videoCapture = new VideoCaptureDeviceCV(i, this);
                        try {
                            videoCapture.open();
                        } catch (IllegalArgumentException e) {
                            // Ignore
                        }
                        if (videoCapture.connected()) {
                            videoCapture.close();
                            devices.add(videoCapture);
                        } else {
                            break;
                        }
                    } catch (Exception e) {
                        // Ignore
                        logger.error(e.getMessage(), e);
                        break;
                    }
                i++;
            }
            return devices;
        }

        @Override
        public VideoCaptureDeviceCV createDevice(String deviceName) {
            if (WaitingForConnection.equals(deviceName)) {
                if (UseVideoInput) {
                    return new VideoCaptureDeviceCV(deviceName, this);
                } else {
                    return new VideoCaptureDeviceCV(0, this);
                }
            } else {
                if (UseVideoInput) {
                    return new VideoCaptureDeviceCV(deviceName, this);
                } else {
                    throw new IllegalArgumentException(deviceName);
                }
            }
        }
    }

    public static MyDeviceFactory getDeviceFactory(Devices devices, Configuration configuration) {
        return new MyDeviceFactory(DeviceClassName, devices, configuration);
    }

    private int deviceId;
    private String deviceName;
    private final MyDeviceFactory factory;
    private final VideoCapture videoCapture;
    Size captureSize = DefaultResolution;
    double fps = 0.0;

    final Mat mat = new Mat();

    private VideoCaptureDeviceCV(String deviceName, MyDeviceFactory factory) {
        this.deviceName = deviceName;
        this.factory = factory;
        this.videoCapture = new VideoCapture();
        this.deviceId = UseVideoInput ? VideoCaptureDeviceVideoInput.getDeviceIDFromName(deviceName)
                : Integer.parseInt(DeviceCache.getDeviceName(deviceName));
    }

    private VideoCaptureDeviceCV(int deviceId, MyDeviceFactory factory) {
        this.deviceName = Integer.toString(deviceId);
        this.factory = factory;
        this.videoCapture = new VideoCapture();
        this.deviceId = deviceId;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName, Integer.toString(deviceId));
    }

    @Override
    public String getName() {
        return "OpenCV Video Capture " + this.deviceName;
    }

    @Override
    public boolean isWireless() {
        return false;
    }

    @Override
    public BatteryLevel batteryLevel() {
        return BatteryLevel.High;
    }

    @Override
    public void open() {
        if (!connected()) {
            connect();
        }
        if (connected()) {
            videoCapture.open(deviceId);
            if (!videoCapture.isOpened()) {
                throw new IllegalArgumentException("Camera not opened: " + getClass().getName() + ":" + deviceId);
            }
            updateCameraProps();
        }
    }

    private void updateCameraProps() {
        captureSize.width((int) videoCapture.get(CAP_PROP_FRAME_WIDTH));
        captureSize.height((int) videoCapture.get(CAP_PROP_FRAME_HEIGHT));
        fps = videoCapture.get(opencv_videoio.CAP_PROP_FPS);
    }

    @Override
    public boolean connected() {
        if (WaitingForConnection.equals(deviceName)) {
            return connect();
        } else {
            return true;
        }
    }

    private boolean connect() {
        List<String> devicePaths = factory.getDevices();
        if (!devicePaths.isEmpty()) {
            deviceName = DeviceCache.getDeviceName(devicePaths.get(0));
            if (WaitingForConnection.equals(deviceName)) {
                return false;
            } else {
                deviceId = VideoCaptureDeviceVideoInput.getDeviceIDFromName(deviceName);
                factory.connectDevice(this);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean active() {
        return videoCapture.isOpened();
    }

    @Override
    public Size resolution() {
        return captureSize;
    }

    @Override
    public ResolutionList getResolutions() {
        return new ResolutionList(captureSize);
    }

    @Override
    public void resolution(Size size) {
        if (!active()) {
            throw new IllegalStateException("Camera not open");
        }
        videoCapture.set(CAP_PROP_FRAME_WIDTH, size.width());
        videoCapture.set(CAP_PROP_FRAME_HEIGHT, size.height());

        double actual = videoCapture.get(opencv_videoio.CAP_PROP_FPS);
        if (actual > 0.0 && fps == 0.0) {
            fps(actual);
        } else if (fps > 0.0) {
            // Try to set a fixed fps, better than frame rate drops
            videoCapture.set(opencv_videoio.CAP_PROP_FPS, fps);
            fps(fps);
        }

        updateCameraProps();

    }

    @Override
    public void fps(double fps) {
        this.fps = fps;
        if (fps > 0.0) {
            // Try to set a fixed exposure time to
            // avoid frame rate drops due to exposure adjustment
            videoCapture.set(opencv_videoio.CAP_PROP_FPS, fps);
            videoCapture.set(opencv_videoio.CAP_PROP_EXPOSURE, 1.0 / fps);
        }
    }

    @Override
    public double fps() {
        return fps;
    }

    @Override
    public Iterator<Mat> iterator() {
        return new FrameIterator();
    }

    private class FrameIterator implements Iterator<Mat> {
        Mat f = null;

        private Mat read() {
            videoCapture.grab();
            // hangs here on surprise removal
            // but interrupting the thread still works
            videoCapture.retrieve(mat);
            return mat;
        }

        private Mat getMat() {
            while (f == null) {
                f = read();
            }
            return f;
        }

        @Override
        public boolean hasNext() {
            if (!videoCapture.isOpened() || videoCapture.isNull()) {
                return false;
            }
            f = getMat();
            return mat != null;
        }

        @Override
        public Mat next() {
            if (!videoCapture.isOpened() || videoCapture.isNull()) {
                throw new NoSuchElementException();
            }

            if (f == null) {
                return getMat();
            } else {
                Mat cached = f;
                f = null;
                return cached;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void close() {
        try {
            videoCapture.release();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
