package teaselib.core.devices.video;

import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_WIDTH;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class VideoCaptureDeviceCV implements VideoCaptureDevice {
    private static final Logger logger = LoggerFactory
            .getLogger(VideoCaptureDeviceCV.class);

    private static final String DeviceClassName = "JavaCVVideoCapture";

    public static final DeviceFactory<VideoCaptureDevice> Factory = new DeviceFactory<VideoCaptureDevice>(
            DeviceClassName) {
        @Override
        public List<String> enumerateDevicePaths(
                Map<String, VideoCaptureDevice> deviceCache) {
            List<String> deviceNames = new ArrayList<String>();
            deviceNames.addAll(VideoCaptureDeviceCV.getDevicesPaths());
            return deviceNames;
        }

        @Override
        public VideoCaptureDevice createDevice(String deviceName) {
            return VideoCaptureDeviceCV.get(deviceName);
        }

    };

    final int device;
    final VideoCapture videoCapture;
    final Mat mat = new Mat();
    Size captureSize = DefaultResolution;
    double fps = 0.0;

    private static List<VideoCapture> devices = new ArrayList<VideoCapture>();

    public static Set<String> getDevicesPaths() {
        int i = 0;
        Set<String> devicePaths = new LinkedHashSet<String>();
        for (@SuppressWarnings("unused")
        // TODO Enumerate to device map, to have devices available
        VideoCapture videoCapture : getCaptureDevices()) {
            devicePaths.add(DeviceCache.createDevicePath(
                    VideoCaptureDeviceCV.DeviceClassName,
                    Integer.toString(i++)));
        }
        return devicePaths;
    }

    static List<VideoCapture> getCaptureDevices() {
        int i = 0;
        while (true) {
            // Only add new devices, because we want them to be singletons
            if (i >= devices.size())
                // Detect new devices
                try {
                @SuppressWarnings("resource")
                VideoCapture videoCapture = new VideoCapture();
                openDevice(videoCapture, i);
                if (videoCapture.isOpened()) {
                videoCapture.release();
                devices.add(videoCapture);
                } else {
                videoCapture.release();
                videoCapture.close();
                break;
                }
                } catch (Exception e) {
                // Ignore
                logger.error(e.getMessage(), e);
                break;
                }
            else {
                // Remove removed devices
                while (i < devices.size()) {
                    VideoCapture videoCapture = devices.get(i);
                    if (!videoCapture.isOpened()) {
                        // ... but on surprise removal
                        devices.remove(i);
                        try {
                            videoCapture.release();
                            videoCapture.close();
                        } catch (Exception e) {
                            // Ignore
                            logger.error(e.getMessage(), e);
                        }
                        // After removing an entry from the list,
                        // Repeat without incrementing the index
                        continue;
                    }
                    // End device enumeration
                    break;
                }
            }
            i++;
        }
        return devices;
    }

    private static void openDevice(VideoCapture videoCapture, int i) {
        // videoCapture.open(opencv_videoio.CV_CAP_MSMF + i);
        videoCapture.open(i);
    }

    public static VideoCaptureDeviceCV get(String name) {
        return get(Integer.parseInt(name));
    }

    public static VideoCaptureDeviceCV get(int n) {
        if (n >= devices.size()) {
            getCaptureDevices();
            if (n >= devices.size()) {
                // default to previous device if camera has been removed
                n = devices.size() - 1;
            }
        }
        return new VideoCaptureDeviceCV(n);
    }

    private VideoCaptureDeviceCV(String id) throws Exception {
        this(Integer.parseInt(DeviceCache.getDeviceName(id)));
    }

    private VideoCaptureDeviceCV(int device) {
        this.device = device;
        videoCapture = devices.get(device);
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                Integer.toString(device));
    }

    @Override
    public String getName() {
        return "OpenCV Video Capture #" + this.device;
    }

    @Override
    public void open() {
        if (!videoCapture.isOpened()) {
            videoCapture.open(device);
        }
        if (!videoCapture.isOpened()) {
            throw new IllegalArgumentException("Camera not opened: "
                    + getClass().getName() + ":" + device);
        }
        captureSize.width((int) videoCapture.get(CAP_PROP_FRAME_WIDTH));
        captureSize.height((int) videoCapture.get(CAP_PROP_FRAME_HEIGHT));
        fps = videoCapture.get(opencv_videoio.CAP_PROP_FPS);
    }

    @Override
    public boolean connected() {
        return videoCapture.isOpened();
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
        videoCapture.set(CAP_PROP_FRAME_HEIGHT, size.width());
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
