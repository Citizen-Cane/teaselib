package teaselib.core.devices.video;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.videoInputLib.videoInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;
import teaselib.video.VideoCaptureDevices;

/**
 * videoInput-based implementation of video capture device. Has some issues (fails tests) but on the other hand the
 * videoInput interface has some features opencv hasn't, so let's keep this for now as a backup.
 * 
 * @author Citizen-Cane
 *
 */
public class VideoCaptureDeviceVideoInput extends VideoCaptureDevice /* extends WiredDevice */ {
    private static final Logger logger = LoggerFactory.getLogger(VideoCaptureDeviceVideoInput.class);

    private static final String DeviceClassName = "JavaCVVideoInput";

    private static final class MyDeviceFactory extends DeviceFactory<VideoCaptureDeviceVideoInput> {
        private MyDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
            super(deviceClass, devices, configuration);
        }

        @Override
        public List<String> enumerateDevicePaths(Map<String, VideoCaptureDeviceVideoInput> deviceCache) {
            List<String> deviceNames = enumerateVideoInputDevices();
            VideoCaptureDevices.sort(deviceNames);
            List<String> devicePaths = new ArrayList<>(deviceNames.size());
            for (String deviceName : deviceNames) {
                devicePaths.add(DeviceCache.createDevicePath(DeviceClassName, deviceName));
            }
            return devicePaths;
        }

        @Override
        public VideoCaptureDeviceVideoInput createDevice(String deviceName) {
            return new VideoCaptureDeviceVideoInput(deviceName, this);
        }
    }

    public static MyDeviceFactory getDeviceFactory(Devices devices, Configuration configuration) {
        return new MyDeviceFactory(DeviceClassName, devices, configuration);
    }

    static List<String> enumerateVideoInputDevices() {
        videoInput.setVerbose(false);
        videoInput.setComMultiThreaded(false);
        int n = videoInput.listDevices(true); // no debug output
        List<String> deviceNames = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String deviceName = videoInput.getDeviceName(i).getString();
            deviceNames.add(deviceName);
        }
        return deviceNames;
    }

    static int getDeviceIDFromName(String deviceName) {
        int n = videoInput.listDevices(false); // no debug output
        for (int i = 0; i < n; i++) {
            if (deviceName.equals(videoInput.getDeviceName(i).getString())) {
                return i;
            }
        }
        return -1;
    }

    private int deviceId;
    private String deviceName;
    private final MyDeviceFactory factory;
    private videoInput vi = null;
    Mat mat = new Mat();
    Size captureSize = DefaultResolution;
    double fps = 0.0;

    private VideoCaptureDeviceVideoInput(String deviceName, MyDeviceFactory factory) {
        if (deviceName.equals(Device.WaitingForConnection)) {
            this.deviceId = Integer.MIN_VALUE;
        } else {
            this.deviceId = getDeviceIDFromName(deviceName);
        }
        this.deviceName = deviceName;
        this.factory = factory;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName, deviceName);
    }

    @Override
    public String getName() {
        return deviceName;
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
            if (vi == null) {
                vi = new videoInput();
                vi.setUseCallback(true);
                vi.setAutoReconnectOnFreeze(deviceId, true, 100);
            }

            vi.setupDevice(deviceId, DefaultResolution.width(), DefaultResolution.height());
            updateCameraProps();
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
        List<String> devicePaths = factory.getDevices();
        if (!devicePaths.isEmpty()) {
            deviceName = DeviceCache.getDeviceName(devicePaths.get(0));
            if (WaitingForConnection.equals(deviceName)) {
                return false;
            } else {
                deviceId = getDeviceIDFromName(deviceName);
                factory.connectDevice(this);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean active() {
        return connected() && vi != null && vi.isDeviceSetup(deviceId);
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

        vi.stopDevice(deviceId);
        vi.setupDevice(deviceId, size.width(), size.height());

        if (!active()) {
            throw new IllegalArgumentException("Camera not opened: " + getDevicePath() + ":" + deviceId);
        }

        updateCameraProps();
    }

    private void updateCameraProps() {
        int width = vi.getWidth(deviceId);
        int height = vi.getHeight(deviceId);
        captureSize = new Size(width, height);
        mat = new Mat(captureSize.height(), captureSize.width(), opencv_core.CV_8UC3);
    }

    @Override
    public void fps(double fps) {
        this.fps = fps;
        vi.setIdealFramerate(deviceId, (int) fps);
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
            mat.data(vi.getPixels(deviceId, false, true));
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
            if (!active()) {
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
            vi.stopDevice(deviceId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
