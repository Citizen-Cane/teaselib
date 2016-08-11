package teaselib.core.devices.video;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;

import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceWebcamCapture implements VideoCaptureDevice {
    private static final Logger logger = LoggerFactory
            .getLogger(VideoCaptureDeviceWebcamCapture.class);

    private static final String DeviceClassName = "WebcamCapture";

    public static final DeviceFactory<VideoCaptureDeviceWebcamCapture> Factory = new DeviceFactory<VideoCaptureDeviceWebcamCapture>(
            DeviceClassName) {
        @Override
        public List<String> enumerateDevicePaths(
                Map<String, VideoCaptureDeviceWebcamCapture> deviceCache) {
            Map<String, VideoCaptureDeviceWebcamCapture> newDevices = new LinkedHashMap<String, VideoCaptureDeviceWebcamCapture>();
            try {
                List<Webcam> webcams = Webcam.getWebcams();
                for (Webcam webcam : webcams) {
                    VideoCaptureDeviceWebcamCapture device = new VideoCaptureDeviceWebcamCapture(
                            webcam);
                    String devicePath = device.getDevicePath();
                    if (deviceCache.containsKey(devicePath)) {
                        newDevices.put(devicePath, deviceCache.get(devicePath));
                    } else {
                        newDevices.put(devicePath, device);
                    }
                }
                if (newDevices.isEmpty()) {
                    // TODO Change the key to default on connect, or what?
                    // TODO remove on connect, add real device name
                    VideoCaptureDeviceWebcamCapture device = new VideoCaptureDeviceWebcamCapture(
                            null);
                    String devicePath = device.getDevicePath();
                    newDevices.put(devicePath, device);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            deviceCache.clear();
            for (Entry<String, VideoCaptureDeviceWebcamCapture> entry : newDevices
                    .entrySet()) {
                deviceCache.put(entry.getKey(), entry.getValue());
            }
            return new ArrayList<String>(deviceCache.keySet());
        }

        @Override
        public VideoCaptureDeviceWebcamCapture createDevice(String deviceName) {
            throw new UnsupportedOperationException();
        }
    };

    Webcam webcam;
    String name;

    WebcamDiscoveryListener discoveryListener = new WebcamDiscoveryListener() {
        @Override
        public void webcamFound(WebcamDiscoveryEvent event) {
            try {
                Webcam newWebcam = event.getWebcam();
                if (webcam == null) {
                    claim(newWebcam);
                } else {
                    // Same as before and not claimed by another discovery
                    // listener yet
                    boolean claimed = webcam.isOpen();
                    if (name.equals(newWebcam.getName()) && !claimed) {
                        claim(newWebcam);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        private void claim(final Webcam newWebcam) {
            Dimension resolution = webcam.getViewSize();
            webcam = newWebcam;
            resolutions = buildResolutionList();
            webcam.open();
            webcam.setViewSize(resolution);
            logger.info(name + " connected");
            Factory.getDevices();
        }

        @Override
        public void webcamGone(WebcamDiscoveryEvent event) {
            try {
                if (event.getWebcam().equals(webcam)) {
                    logger.info(getName() + " disconnected");
                    webcam.close();
                    webcam = null;
                    // TODO store name and release webcam
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    };

    Mat mat;
    ResolutionList resolutions;
    Size captureSize; // Keep to restore size when old webcam is gone
    double fps;

    private VideoCaptureDeviceWebcamCapture(Webcam webcam) {
        this.webcam = webcam;
        if (webcam != null) {
            this.name = webcam.getName();
        } else {
            this.name = Device.WaitingForConnection;
        }
        resolutions = null;
    }

    private ResolutionList buildResolutionList() {
        ResolutionList resolutions = new ResolutionList();
        for (Dimension dimension : webcam.getViewSizes()) {
            resolutions.add(new Size(dimension.width, dimension.height));
        }
        if (resolutions.size() == 0) {
            resolutions.add(DefaultResolution);
        } else {
            // OpenImagJ capture driver doesn't detect resolutions (yet) (just
            // on Windows?)
            // https://github.com/sarxos/webcam-capture/issues/272
            // Therefore we have to return DefaultResolution
            Size peek = resolutions.get(0);
            if (isFakeResolution(peek)) {
                resolutions.clear();
                resolutions.add(DefaultResolution);
            }
            peek.close();
        }
        return resolutions;
    }

    private static boolean isFakeResolution(Size resolution) {
        return resolution.width() == 176 && resolution.height() == 144;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                webcam != null ? webcam.getName() : WaitingForConnection);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void open() {
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
    public void resolution(Size size) {
        if (!getResolutions().contains(size)) {
            throw new IllegalArgumentException(
                    size.width() + "," + size.height());
        }
        boolean open = webcam.isOpen();
        if (open) {
            if (size != DefaultResolution && size.width() != captureSize.width()
                    && size.height() != captureSize.height()) {
                webcam.close();
            }
        }
        webcam.setViewSize(new Dimension(size.width(), size.height()));
        this.captureSize = size;
        if (open && !webcam.isOpen()) {
            webcam.open();
            fps = webcam.getFPS();
        }
    }

    @Override
    public ResolutionList getResolutions() {
        if (resolutions == null) {
            resolutions = buildResolutionList();
        }
        return resolutions;
    }

    @Override
    public boolean connected() {
        return webcam != null;
    }

    @Override
    public boolean active() {
        return connected() && webcam.isOpen();
    }

    @Override
    public Size resolution() {
        Dimension viewSize = webcam.getViewSize();
        Size resolution = new Size(viewSize.width, viewSize.height);
        if (isFakeResolution(resolution)) {
            return DefaultResolution;
        } else {
            return resolution;
        }
    }

    @Override
    public void fps(double fps) {
        // TODO There's no way to set the frame rate
        this.fps = webcam.getFPS();
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
    public void close() {
        try {
            webcam.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
