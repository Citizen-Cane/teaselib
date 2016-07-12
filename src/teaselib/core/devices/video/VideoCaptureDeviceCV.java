package teaselib.core.devices.video;

import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_WIDTH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;

import teaselib.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceCV implements VideoCaptureDevice {
    private static final String DeviceClassName = "JavaCVVideoCapture";

    public static final DeviceCache.Factory<VideoCaptureDevice> Factory = new DeviceCache.Factory<VideoCaptureDevice>() {
        @Override
        public String getDeviceClass() {
            return VideoCaptureDeviceCV.DeviceClassName;
        }

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
    };

    final int device;
    final VideoCapture videoCapture;
    final Mat mat = new Mat();
    Size captureSize;

    double fps;

    org.bytedeco.javacv.FrameGrabber.Exception e = null;

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
                TeaseLib.instance().log.error(devices, e);
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
                            TeaseLib.instance().log.error(devices, e);
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
    public void open(Size size) {
        if (!videoCapture.isOpened()) {
            videoCapture.open(device);
        }
        if (!videoCapture.isOpened()) {
            throw new IllegalArgumentException("Camera not opened: "
                    + getClass().getName() + ":" + device);
        }
        setResolution(getResolutions().get(0));
    }

    @Override
    public boolean active() {
        return videoCapture.isOpened();
    }

    @Override
    public Size captureSize() {
        return captureSize;
    }

    @Override
    public List<Size> getResolutions() {
        return Arrays.asList(captureSize);
    }

    @Override
    public void setResolution(Size size) {
        videoCapture.set(CAP_PROP_FRAME_WIDTH, size.width());
        videoCapture.set(CAP_PROP_FRAME_HEIGHT, size.width());
        fps = videoCapture.get(opencv_videoio.CAP_PROP_FPS);
        if (fps > 0.0) {
            // Try to set a fixed fps, better than frame rate drops
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
    public void release() {
        try {
            videoCapture.release();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
    }
}
