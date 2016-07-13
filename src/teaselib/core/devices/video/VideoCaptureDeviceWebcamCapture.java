package teaselib.core.devices.video;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;

import teaselib.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.video.ResolutionList;
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
                    Dimension resolution = webcam.getViewSize();
                    webcam = newWebcam;
                    resolutions = buildResolutionList();
                    webcam.open();
                    webcam.setViewSize(resolution);
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
    ResolutionList resolutions;
    Size captureSize; // Keep to restore size when old webcam is gone
    double fps;

    private VideoCaptureDeviceWebcamCapture(Webcam webcam) {
        this.webcam = webcam;
        this.name = webcam.getName();
        resolutions = buildResolutionList();
    }

    private ResolutionList buildResolutionList() {
        ResolutionList resolutions = new ResolutionList();
        for (Dimension dimension : webcam.getViewSizes()) {
            resolutions.add(new Size(dimension.width, dimension.height));
        }
        if (resolutions.size() == 0) {
            resolutions.add(DefaultResolution);
        } else {
            // OpenImagJ capture driver doesn't detect resolutions (yet) (just on Windows?)
            // https://github.com/sarxos/webcam-capture/issues/272
            // Therefore we have to return DefaultResolution
            Size peek = resolutions.get(0);
            if (isFakeResolution(peek)) {
                resolutions.clear();
                resolutions.add(DefaultResolution);
            }
        }
        return resolutions;
    }

    private boolean isFakeResolution(Size resolution) {
        return resolution.width() == 176 && resolution.height() == 144;
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
    public void open(Size resolution) {
        if (resolution != DefaultResolution) {
            setResolution(resolution);
        }
        if (!webcam.isOpen()) {
            webcam.open();
        }
        if (!webcam.isOpen()) {
            throw new IllegalArgumentException(
                    "Camera not opened: " + getName());
        }
        Dimension captureSize = webcam.getViewSize();
        this.captureSize = new Size(captureSize.width, captureSize.height);
        if (webcam.isOpen()) {
            fps = webcam.getFPS();
        }
    }

    @Override
    public void setResolution(Size size) {
        if (!getResolutions().contains(size)) {
            throw new IllegalArgumentException(
                    size.width() + "," + size.height());
        }
        boolean open = webcam.isOpen();
        if (open) {
            webcam.close();
        }
        webcam.setViewSize(new Dimension(size.width(), size.height()));
        this.captureSize = size;
        if (open) {
            webcam.open();
            fps = webcam.getFPS();
        }
    }

    @Override
    public ResolutionList getResolutions() {
        return resolutions;
    }

    @Override
    public boolean active() {
        return webcam.isOpen();
    }

    @Override
    public Size captureSize() {
        Dimension viewSize = webcam.getViewSize();
        Size resolution = new Size(viewSize.width, viewSize.height);
        if (isFakeResolution(resolution)) {
            return DefaultResolution;
        } else {
            return resolution;
        }
    }

    @Override
    public double fps() {
        return fps;
    }

    @SuppressWarnings("resource")
    private Mat read() {
        ByteBuffer image = webcam.getImageBytes();
        mat = new Mat(captureSize.height(), captureSize.width(),
                opencv_core.CV_8UC3, new BytePointer(image));
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
            if (!webcam.isOpen() || webcam == null) {
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
