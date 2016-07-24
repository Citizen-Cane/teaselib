package teaselib.core.devices.video;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.videoInputLib.videoInput;

import teaselib.TeaseLib;
import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;

public class VideoCaptureDeviceVideoInput implements VideoCaptureDevice {
    private static final String DeviceClassName = "JavaCVVideoInput";

    public static final DeviceCache.Factory<VideoCaptureDevice> Factory = new DeviceCache.Factory<VideoCaptureDevice>() {
        final Map<String, VideoCaptureDeviceVideoInput> devices = new LinkedHashMap<String, VideoCaptureDeviceVideoInput>();

        @Override
        public String getDeviceClass() {
            return VideoCaptureDeviceVideoInput.DeviceClassName;
        }

        @Override
        public List<String> getDevices() {
            videoInput.setVerbose(false);
            videoInput.setComMultiThreaded(false);
            int n = videoInput.listDevices(true); // no debug output
            List<String> deviceNames = new ArrayList<String>(n);
            for (int i = 0; i < n; i++) {
                String deviceName = videoInput.getDeviceName(i).getString();
                deviceNames.add(DeviceCache.createDevicePath(DeviceClassName,
                        deviceName));

            }
            if (deviceNames.isEmpty()) {
                deviceNames.add(DeviceCache.createDevicePath(DeviceClassName,
                        Device.WaitingForConnection));
            }
            return deviceNames;
        }

        @Override
        public VideoCaptureDevice getDevice(String devicePath) {
            if (devices.containsKey(devicePath)) {
                return devices.get(devicePath);
            } else {
                String deviceName = DeviceCache.getDeviceName(devicePath);
                VideoCaptureDeviceVideoInput device = new VideoCaptureDeviceVideoInput(
                        deviceName);
                devices.put(devicePath, device);
                return device;
            }
        }

    };

    private static int getDeviceIDFromName(String deviceName) {
        // returns always -1
        // videoInput.getDeviceIDFromName(deviceName);
        int n = videoInput.listDevices(false); // no debug output
        for (int i = 0; i < n; i++) {
            if (deviceName.equals(videoInput.getDeviceName(i).getString())) {
                return i;
            }
        }
        return -1;
    }

    // TODO camera discovery listener
    // TODO Default device connect to discovered camera
    // TODO set & get frame rate

    final int deviceId;
    final String deviceName;
    private videoInput vi = null;
    Mat mat = new Mat();
    Size captureSize = DefaultResolution;
    double fps = 0.0;

    private VideoCaptureDeviceVideoInput(String deviceName) {
        if (deviceName.equals(Device.WaitingForConnection)) {
            this.deviceId = Integer.MIN_VALUE;
        } else {
            this.deviceId = getDeviceIDFromName(deviceName);
        }
        this.deviceName = deviceName;
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
    public void open(Size size) {
        if (vi == null) {
            vi = new videoInput();
            vi.setUseCallback(true);
            vi.setAutoReconnectOnFreeze(deviceId, true, 100);
        }
        if (size != DefaultResolution) {
            setResolution(size);
        } else if (!vi.isDeviceSetup(deviceId)) {
            if (!vi.setupDevice(deviceId)) {
                throw new IllegalArgumentException("Camera not opened: "
                        + getDevicePath() + ":" + deviceId);
            }
        }
        setData(vi.getWidth(deviceId), vi.getHeight(deviceId));
    }

    @Override
    public boolean connected() {
        return deviceId != Integer.MIN_VALUE;
    }

    @Override
    public boolean active() {
        return vi.isDeviceSetup(deviceId);
    }

    @Override
    public Size captureSize() {
        return captureSize;
    }

    @Override
    public ResolutionList getResolutions() {
        return new ResolutionList(captureSize);
    }

    @Override
    public void setResolution(Size size) {
        if (!getResolutions().contains(size)) {
            throw new IllegalArgumentException(
                    size.width() + "," + size.height());
        }
        vi.setupDevice(deviceId, size.width(), size.height());
        if (!active()) {
            throw new IllegalArgumentException(
                    "Camera not opened: " + getDevicePath() + ":" + deviceId);
        }
        setData(vi.getWidth(deviceId), vi.getHeight(deviceId));
    }

    private void setData(int width, int height) {
        fps = 0.0;
        // fps = vi.get(opencv_videoio.CAP_PROP_FPS);
        if (fps > 0.0) {
            // TODO set exposure
            // videoCapture.set(opencv_videoio.CAP_PROP_EXPOSURE, 1.0 / fps);
        }
        captureSize = new Size(width, height);
        mat = new Mat(captureSize.height(), captureSize.width(),
                opencv_core.CV_8UC3);
    }

    @Override
    public double fps() {
        return fps;
    }

    private Mat read() {
        mat.data(vi.getPixels(deviceId, false, true));
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
    public void release() {
        try {
            vi.stopDevice(deviceId);
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
    }
}
