package teaselib.core.devices.motiondetection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.VideoRenderer;
import teaselib.core.configuration.Configuration;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.devices.Devices;
import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.Pose;
import teaselib.motiondetection.Proximity;
import teaselib.motiondetection.ViewPoint;
import teaselib.video.VideoCaptureDevice;

/**
 * @author Citizen-Cane
 * 
 *         Bullet-Proof motion detector:
 *         <p>
 *         Motion is detected via motion contours from background subtraction. Sensitivity is implemented by applying a
 *         structuring element to the motion contours.
 *         <p>
 *         Absence of motion is measured by measuring the distance of tracking points from positions set when motion
 *         last stopped. Motion is detected when the tracking points move too far away from their start points.
 *         <p>
 *         Blinking eyes are detected by removing small circular motion contours before calculating the motion region.
 *         <p>
 *         The camera should be placed on top of the monitor with no tilt.
 *         <p>
 *         The detector is stable, but still can be cheated/misled. Av oid:
 *         <p>
 *         pets.
 *         <p>
 *         audience within the field of view-
 *         <p>
 *         light sources from moving cars or traffic lights.
 * 
 */
public class MotionDetectorJavaCV extends MotionDetector /* extends WiredDevice */ {
    static final Logger logger = LoggerFactory.getLogger(MotionDetectorJavaCV.class);

    private static final String DeviceClassName = "MotionDetectorJavaCV";

    private static final class MyDeviceFactory extends DeviceFactory<MotionDetectorJavaCV> {
        private MyDeviceFactory(String deviceClass, Devices devices, Configuration configuration) {
            super(deviceClass, devices, configuration);
        }

        @Override
        public List<String> enumerateDevicePaths(Map<String, MotionDetectorJavaCV> deviceCache) {
            List<String> deviceNames = new ArrayList<>();
            Set<String> videoCaptureDevicePaths = devices.get(VideoCaptureDevice.class).getDevicePaths();
            for (String videoCaptureDevicePath : videoCaptureDevicePaths) {
                deviceNames.add(DeviceCache.createDevicePath(DeviceClassName, videoCaptureDevicePath));
            }
            return deviceNames;
        }

        @Override
        public MotionDetectorJavaCV createDevice(String deviceName) {
            DeviceCache<VideoCaptureDevice> videoCaptureDevices = devices.get(VideoCaptureDevice.class);
            return new MotionDetectorJavaCV(videoCaptureDevices.getDevice(deviceName));
        }
    }

    public static MyDeviceFactory getDeviceFactory(Devices devices, Configuration configuration) {
        return new MyDeviceFactory(DeviceClassName, devices, configuration);
    }

    public static final Set<Feature> Features = EnumSet.of(Feature.Motion, Feature.Presence);
    static final double DesiredFps = 30;

    private final MotionDetectorCaptureThread captureThread;

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        super();
        captureThread = new MotionDetectorCaptureThread(videoCaptureDevice, DesiredFps);
        setSensitivity(MotionSensitivity.Normal);
        setViewPoint(ViewPoint.EyeLevel);

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        captureThread.setDaemon(false);
        captureThread.setName(getDevicePath());
        captureThread.start();
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName, captureThread.videoCaptureDevice.getDevicePath());
    }

    @Override
    public String getName() {
        return "Motion Detector";
    }

    @Override
    public MotionSensitivity getSensitivity() {
        return captureThread.motionSensitivity;
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensitivity) {
        captureThread.setSensitivity(motionSensitivity);
    }

    @Override
    public ViewPoint getViewPoint() {
        return captureThread.viewPoint;
    }

    @Override
    public void setViewPoint(ViewPoint pointOfView) {
        captureThread.setPointOfView(pointOfView);
    }

    @Override
    public VideoRenderer getVideoRenderer() {
        return captureThread.videoRenderer;
    }

    @Override
    public void setVideoRenderer(VideoRenderer videoRenderer) {
        captureThread.videoRenderer = videoRenderer;
    }

    @Override
    public Set<Feature> getFeatures() {
        return Features;
    }

    @Override
    public boolean await(double amount, Presence change, double timeSpanSeconds, double timeoutSeconds) {
        if (!active()) {
            return handleInactive(timeoutSeconds);
        }

        captureThread.debugWindowTimeSpan = timeSpanSeconds;
        try {
            return captureThread.motion.await(amount, change, timeSpanSeconds, timeoutSeconds);
        } finally {
            captureThread.debugWindowTimeSpan = MotionRegionDefaultTimespan;
        }
    }

    @Override
    public Gesture await(List<Gesture> expected, double timeoutSeconds) {
        if (!active()) {
            handleInactive(timeoutSeconds);
            return Gesture.None;
        }

        captureThread.gesture.await(expected, timeoutSeconds);
        return captureThread.gesture.get();
    }

    @Override
    public boolean await(Proximity expected, double timeoutSeconds) {
        if (!active()) {
            return handleInactive(timeoutSeconds);
        }

        return captureThread.proximity.await(expected, timeoutSeconds);
    }

    @Override
    public boolean await(Pose expected, double timeoutSeconds) {
        if (!active()) {
            return handleInactive(timeoutSeconds);
        }

        return captureThread.pose.await(expected, timeoutSeconds);
    }

    private boolean handleInactive(double timeoutSeconds) {
        if (timeoutSeconds <= 0.0) {
            return false;
        } else if (timeoutSeconds < Double.MAX_VALUE) {
            double reducedTimeoutSeconds = timeoutSeconds * 0.5;
            boolean fakedResult = true;
            try {
                Thread.sleep((long) (reducedTimeoutSeconds * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ScriptInterruptedException(e);
            }
            return fakedResult;
        } else {
            throw new IllegalStateException(getClass().getName() + " not active");
        }
    }

    @Override
    public boolean isWireless() {
        return captureThread.videoCaptureDevice.isWireless();
    }

    @Override
    public BatteryLevel batteryLevel() {
        return captureThread.videoCaptureDevice.batteryLevel();
    }

    protected double fps() {
        return captureThread.fps();
    }

    @Override
    public boolean connected() {
        return captureThread.videoCaptureDevice.connected();
    }

    @Override
    public boolean active() {
        return captureThread.active();
    }

    @Override
    public void stop() {
        captureThread.stopCapture();
    }

    @Override
    public void start() {
        captureThread.startCapture();
    }

    @Override
    public void close() {
        captureThread.interrupt();
        try {
            captureThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
