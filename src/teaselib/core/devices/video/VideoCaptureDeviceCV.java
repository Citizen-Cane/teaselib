package teaselib.core.devices.video;

import static org.bytedeco.javacpp.opencv_videoio.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.bridj.Platform;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;
import teaselib.video.VideoCaptureDevices;

public class VideoCaptureDeviceCV implements VideoCaptureDevice {
    private static final Logger logger = LoggerFactory
            .getLogger(VideoCaptureDeviceCV.class);

    private static final String DeviceClassName = "JavaCVVideoCapture";

    private final static boolean useVideoInput = Platform.isWindows();

    public static final DeviceFactory<VideoCaptureDevice> Factory = new DeviceFactory<VideoCaptureDevice>(
            DeviceClassName) {

        @Override
        public List<String> enumerateDevicePaths(
                Map<String, VideoCaptureDevice> deviceCache) {
            final List<String> devicePaths;
            if (useVideoInput) {
                List<String> deviceNames = VideoCaptureDeviceVideoInput
                        .enumerateVideoInputDevices();
                VideoCaptureDevices.sort(deviceNames);
                devicePaths = new ArrayList<String>(deviceNames.size());
                for (String deviceName : deviceNames) {
                    devicePaths.add(DeviceCache
                            .createDevicePath(DeviceClassName, deviceName));
                }
            } else {
                int i = 0;
                List<VideoCaptureDeviceCV> captureDevices = getCaptureDevices(
                        deviceCache);
                devicePaths = new ArrayList<String>(captureDevices.size());
                for (VideoCaptureDeviceCV device : captureDevices) {
                    String devicePath = DeviceCache.createDevicePath(
                            VideoCaptureDeviceCV.DeviceClassName,
                            Integer.toString(i++));
                    deviceCache.put(device.getDevicePath(), device);
                    devicePaths.add(devicePath);
                }
                devicePaths.addAll(devicePaths);
            }
            return devicePaths;
        }

        private List<VideoCaptureDeviceCV> getCaptureDevices(
                Map<String, VideoCaptureDevice> deviceCache) {
            int i = 0;
            List<VideoCaptureDeviceCV> devices = new Vector<VideoCaptureDeviceCV>();
            while (true) {
                // Only add new devices, because we want them to be singletons
                if (i >= deviceCache.size())
                    // Detect new devices
                    try {
                        VideoCaptureDeviceCV videoCapture = new VideoCaptureDeviceCV(
                                i);
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
        public VideoCaptureDevice createDevice(String deviceName) {
            if (WaitingForConnection.equals(deviceName)) {
                if (useVideoInput) {
                    return new VideoCaptureDeviceCV(deviceName);
                } else {
                    return new VideoCaptureDeviceCV(0);
                }
            } else {
                if (useVideoInput) {
                    return new VideoCaptureDeviceCV(deviceName);
                } else {
                    throw new IllegalArgumentException(deviceName);
                }
            }
        }

    };

    private int deviceId;
    private String deviceName;
    final VideoCapture videoCapture;
    Size captureSize = DefaultResolution;
    double fps = 0.0;

    final Mat mat = new Mat();

    private VideoCaptureDeviceCV(String deviceName) {
        this.videoCapture = new VideoCapture();
        this.deviceId = useVideoInput
                ? VideoCaptureDeviceVideoInput.getDeviceIDFromName(deviceName)
                : Integer.parseInt(DeviceCache.getDeviceName(deviceName));
        this.deviceName = deviceName;
    }

    private VideoCaptureDeviceCV(int deviceId) {
        this.videoCapture = new VideoCapture();
        this.deviceId = deviceId;
        this.deviceName = Integer.toString(deviceId);
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                Integer.toString(deviceId));
    }

    @Override
    public String getName() {
        return "OpenCV Video Capture " + this.deviceName;
    }

    @Override
    public void open() {
        if (!connected()) {
            connect();
        }
        if (connected()) {
            videoCapture.open(deviceId);
            if (!videoCapture.isOpened()) {
                throw new IllegalArgumentException("Camera not opened: "
                        + getClass().getName() + ":" + deviceId);
            }
            captureSize.width((int) videoCapture.get(CAP_PROP_FRAME_WIDTH));
            captureSize.height((int) videoCapture.get(CAP_PROP_FRAME_HEIGHT));
            fps = videoCapture.get(opencv_videoio.CAP_PROP_FPS);
        }
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
        List<String> devicePaths = Factory.getDevices();
        if (devicePaths.size() > 0) {
            deviceName = DeviceCache.getDeviceName(devicePaths.get(0));
            if (WaitingForConnection.equals(deviceName)) {
                return false;
            } else {
                deviceId = VideoCaptureDeviceVideoInput
                        .getDeviceIDFromName(deviceName);
                Factory.connectDevice(this);
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
        if (!getResolutions().contains(size)) {
            throw new IllegalArgumentException(
                    size.width() + "," + size.height());
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
    }

    @Override
    public void fps(double fps) {
        this.fps = fps;
        if (fps > 0.0) {
            // Try to set a fixed exposure time to
            // avoid frame rate drops due to exposure adjustment
            videoCapture.set(opencv_videoio.CAP_PROP_EXPOSURE, 1.0 / fps);
        }
    }

    @Override
    public double fps() {
        return fps;
    }

    private Mat read() {
        videoCapture.grab();
        // hangs here on surprise removal
        // but interrupting the thread still works
        videoCapture.retrieve(mat);
        return mat;
    }

    @Override
    public Iterator<Mat> iterator() {
        return new FrameIterator();
    }

    private class FrameIterator implements Iterator<Mat> {
        Mat f = null;

        private Mat getMat() {
            while (true) {
                f = read();
                if (f != null) {
                    break;
                } else {
                    continue;
                }
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
