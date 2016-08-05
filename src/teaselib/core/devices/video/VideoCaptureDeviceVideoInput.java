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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.Device;
import teaselib.core.devices.DeviceCache;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;
import teaselib.video.VideoCaptureDevices;

// TODO set & get frame rate
// TODO set exposure
// detect disconnect
// reconnect to same camera -> here
// reconnect to new camera -> application

public class VideoCaptureDeviceVideoInput implements VideoCaptureDevice {
    private static final Logger logger = LoggerFactory
            .getLogger(VideoCaptureDeviceVideoInput.class);

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
            int n = videoInput.listDevices(true); // no
                                                  // debug
                                                  // output
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
        // returns always -1:
        // videoInput.getDeviceIDFromName(deviceName);
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
        }
    }

    @Override
    public boolean connected() {
        if (deviceId == Integer.MIN_VALUE) {
            return connect();
        } else {
            return true;
        }
    }

    private boolean connect() {
        List<String> devicePaths = Factory.getDevices();
        if (devicePaths.size() > 0) {
            if (WaitingForConnection
                    .equals(DeviceCache.getDeviceName(devicePaths.get(0)))) {
                return false;
            } else {
                deviceName = DeviceCache.getDeviceName(
                        VideoCaptureDevices.sort(devicePaths).get(0));
                deviceId = getDeviceIDFromName(deviceName);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean active() {
        return connected() && vi.isDeviceSetup(deviceId);
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
        vi.setupDevice(deviceId, size.width(), size.height());
        if (!active()) {
            throw new IllegalArgumentException(
                    "Camera not opened: " + getDevicePath() + ":" + deviceId);
        }
        setExposure();
        setData(vi.getWidth(deviceId), vi.getHeight(deviceId));
    }

    private void setData(int width, int height) {
        captureSize = new Size(width, height);
        mat = new Mat(captureSize.height(), captureSize.width(),
                opencv_core.CV_8UC3);
    }

    private void setExposure() {
        // set exposure time via Microsoft IAMCameraControl interface
        // based on example code from
        // http://stackoverflow.com/questions/36459563/getting-setting-camera-led-status-light-iusing-videoinput-logitech-c930e
        @SuppressWarnings("unused")
        int CameraControl_Pan = 0;
        @SuppressWarnings("unused")
        int CameraControl_Tilt = 1;
        @SuppressWarnings("unused")
        int CameraControl_Roll = 2;
        @SuppressWarnings("unused")
        int CameraControl_Zoom = 3;
        int CameraControl_Exposure = 4;
        @SuppressWarnings("unused")
        int CameraControl_Iris = 5;
        @SuppressWarnings("unused")
        int CameraControl_Focus = 6;

        int CameraControl_Flags_Auto = 0x0001;
        int CameraControl_Flags_Manual = 0x0002;

        int[] min = { 0 };
        int[] max = { 0 };
        int[] steppingDelta = { 0 };
        int[] currentValue = { 0 };
        int[] flags = { 0 };
        int[] defaultValue = { 0 };

        vi.getVideoSettingCamera(deviceId, CameraControl_Exposure, min, max,
                steppingDelta, currentValue, flags, defaultValue);

        if (currentValue[0] > 0) {
            int exposure;
            if (fps > 0) {
                exposure = (int) (1000.0 / fps);
                if (exposure < min[0]) {
                    exposure = min[0];
                } else if (exposure > max[0]) {
                    exposure = max[0];
                }
            } else {
                exposure = currentValue[0];
            }
            vi.setVideoSettingCamera(deviceId, CameraControl_Exposure, exposure,
                    exposure > 0 ? CameraControl_Flags_Manual
                            : CameraControl_Flags_Auto,
                    false);
        }
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
    public void close() {
        try {
            vi.stopDevice(deviceId);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
