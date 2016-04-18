package teaselib.core.devices;

import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.javacpp.opencv_videoio.CAP_PROP_FRAME_WIDTH;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;

import teaselib.TeaseLib;
import teaselib.core.javacv.Resize;
import teaselib.motiondetection.DeviceCache;

public class VideoCaptureDeviceCV implements VideoCaptureDevice {
    public static final String DeviceClassName = "VideoCaptureDeviceJavaCV";
    final int device;
    final VideoCapture videoCapture;
    final Mat mat = new Mat();

    Size captureSize;
    Size size;
    Resize resize;

    double fps;

    org.bytedeco.javacv.FrameGrabber.Exception e = null;

    private static List<VideoCapture> devices = new ArrayList<>();

    public static Set<String> getDevicesPaths() {
        int i = 0;
        Set<String> devicePaths = new LinkedHashSet<>();
        for (VideoCapture videoCapture : getCaptureDevices()) {
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
                try {
                    VideoCapture videoCapture = new VideoCapture();
                    videoCapture.open(i);
                    if (videoCapture.isOpened()) {
                        devices.add(videoCapture);
                    } else {
                        // videoCapture.close();
                        break;
                    }
                } catch (Exception e) {
                    // Ignore
                    TeaseLib.instance().log.error(devices, e);
                    break;
                }
            else {
                // Devices are never closed...
                VideoCapture videoCapture = devices.get(i);
                if (!videoCapture.isOpened()) {
                    // ... but on surprise removal
                    try {
                        videoCapture.close();
                    } catch (Exception e) {
                        // Ignore
                        TeaseLib.instance().log.error(devices, e);
                    }
                    devices.remove(i);
                    break;
                }
            }
            i++;
        }
        return devices;

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
    public void open(Size size) {
        if (!videoCapture.isOpened()) {
            videoCapture.open(device);
        }
        if (!videoCapture.isOpened()) {
            throw new IllegalArgumentException("Camera not opened: "
                    + getClass().getName() + ":" + device);
        }
        // OpenCV just provides the actual resolution,
        // but not the list of all available resolutions.
        // Therefore we have to try to set the resolution,
        // but we don't know whether the capture device supports it
        // If it doesn't, the capture device frames are resized
        // to match the requested resolution as close as possible
        int captureWidth = (int) videoCapture.get(CAP_PROP_FRAME_WIDTH);
        int captureHeight = (int) videoCapture.get(CAP_PROP_FRAME_HEIGHT);
        if (size.width() > 0 && size.width() != captureWidth) {
            // derive aspect from capture width:
            // Might not be 100% since although a capture device
            // features a 4:3 sensor it may report a 16:9 resolution
            // in order to support Full HD resolutions
            double aspect = (double) (captureHeight) / (double) (captureWidth);
            videoCapture.set(CAP_PROP_FRAME_WIDTH, size.width());
            videoCapture.set(CAP_PROP_FRAME_HEIGHT, size.width() * aspect);
            // update because the capture device
            // may or may not support the requested resolution
            captureWidth = (int) videoCapture.get(CAP_PROP_FRAME_WIDTH);
            captureHeight = (int) videoCapture.get(CAP_PROP_FRAME_HEIGHT);
        }
        this.captureSize = new Size(captureWidth, captureHeight);
        int resizeFactor = Math.max(1, captureWidth / size.width());
        this.size = new Size(captureWidth / resizeFactor,
                captureHeight / resizeFactor);
        resize = new Resize(resizeFactor);
        fps = videoCapture.get(opencv_videoio.CAP_PROP_FPS);
        if (fps > 0.0) {
            // Try to set a fixed fps, better than frame rate drops
            videoCapture.set(opencv_videoio.CAP_PROP_EXPOSURE, 1.0 / fps);
        }
    }

    @Override
    public Size size() {
        return size;
    }

    @Override
    public Size captureSize() {
        return captureSize;
    }

    @Override
    public double fps() {
        return fps;
    }

    private Mat read() {
        videoCapture.grab();
        videoCapture.retrieve(mat);
        return mat;
    }

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.motiondetection.javacv.VideoCaptureDevice#iterator()
     */
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
                }
            }
            if (resize.factor > 1) {
                return resize.update(f);
            } else {
                return f;
            }
        }

        @Override
        public boolean hasNext() {
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

    /*
     * (non-Javadoc)
     * 
     * @see teaselib.motiondetection.javacv.VideoCaptureDevice#close()
     */
    @Override
    public void close() {
        try {
            videoCapture.close();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
    }
}
