package teaselib.core.devices.video;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;

import teaselib.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.core.javacv.ScaleDown;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceWebcamCapture implements VideoCaptureDevice {
    private static final String DeviceClassName = "WebcamCapture";

    public static final DeviceCache.Factory<VideoCaptureDevice> Factory = new DeviceCache.Factory<VideoCaptureDevice>() {
        final Map<String, VideoCaptureDeviceWebcamCapture> devices = new LinkedHashMap<String, VideoCaptureDeviceWebcamCapture>();

        @Override
        public String getDeviceClass() {
            return VideoCaptureDeviceWebcamCapture.DeviceClassName;
        }

        @Override
        public List<String> getDevices() {
            devices.clear();
            try {
                List<Webcam> webcams = Webcam.getWebcams();
                for (Webcam webcam : webcams) {
                    final VideoCaptureDeviceWebcamCapture device = new VideoCaptureDeviceWebcamCapture(
                            webcam);
                    devices.put(device.getDevicePath(), device);
                }
            } catch (Exception e) {
                TeaseLib.instance().log.error(Webcam.class, e);
            }
            return new ArrayList<String>(devices.keySet());
        }

        @Override
        public VideoCaptureDevice getDevice(String devicePath) {
            return devices.get(devicePath);
        }
    };

    Webcam webcam;
    String name;
    WebcamDiscoveryListener discoveryListener = new WebcamDiscoveryListener() {
        @Override
        public void webcamFound(WebcamDiscoveryEvent event) {
            try {
                final Webcam newWebcam = event.getWebcam();
                // Same as before and not claimed by another discovery listener
                if (name.equals(newWebcam.getName()) && !webcam.isOpen()) {
                    webcam = newWebcam;
                    webcam.open();
                    webcam.setViewSize(new Dimension(captureSize.width(),
                            captureSize.height()));
                    TeaseLib.instance().log.info(name + " reconnected");
                }
            } catch (Exception e) {
                TeaseLib.instance().log.error(this, e);
            }
        }

        @Override
        public void webcamGone(WebcamDiscoveryEvent event) {
            try {
                final Webcam gone = event.getWebcam();
                final String goneName = gone.getName();
                if (name.equals(goneName)) {
                    TeaseLib.instance().log.info(goneName + " disconnected");
                    gone.close();
                }
            } catch (Exception e) {
                TeaseLib.instance().log.error(this, e);
            }
        }
    };

    Mat mat;

    Size captureSize;
    Size size;
    ScaleDown resize;

    double fps;

    org.bytedeco.javacv.FrameGrabber.Exception e = null;

    private VideoCaptureDeviceWebcamCapture(Webcam webcam) {
        this.webcam = webcam;
        this.name = webcam.getName();
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName, webcam.getName());
    }

    @Override
    public String getName() {
        return webcam.getName();
    }

    @Override
    public void open(Size size) {
        if (!webcam.isOpen()) {
            webcam.open();
        }
        if (!webcam.isOpen()) {
            throw new IllegalArgumentException(
                    "Camera not opened: " + getName());
        }
        this.size = size;
        setResolution(size);
        final Dimension captureSize = webcam.getViewSize();
        this.captureSize = new Size(captureSize.width, captureSize.height);
    }

    @Override
    public void setResolution(Size size) {
        webcam.setViewSize(new Dimension(size.width(), size.height()));
        fps = webcam.getFPS();
    }

    @Override
    public List<Size> getResolutions() {
        List<Size> resolutions = new Vector<Size>();
        for (Dimension dimension : webcam.getCustomViewSizes()) {
            resolutions.add(new Size(dimension.width, dimension.height));
        }
        return resolutions;
    }

    @Override
    public boolean active() {
        return webcam.isOpen();
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

    @SuppressWarnings("resource")
    private Mat read() {
        ByteBuffer image = webcam.getImageBytes();
        // TODO Set format of mat
        mat = new Mat(new BytePointer(image));
        mat.cols(captureSize.width());
        mat.rows(captureSize.height());
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
            if (resize.factor > 1) {
                return resize.update(f);
            } else {
                return f;
            }
        }

        @Override
        public boolean hasNext() {
            if (!webcam.isOpen() || webcam != null) {
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
            webcam.close();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
    }
}
