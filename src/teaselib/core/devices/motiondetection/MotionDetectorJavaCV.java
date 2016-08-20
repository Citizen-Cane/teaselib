package teaselib.core.devices.motiondetection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.Signal;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;
import teaselib.core.javacv.Copy;
import teaselib.core.javacv.Scale;
import teaselib.core.javacv.ScaleAndMirror;
import teaselib.core.javacv.Transformation;
import teaselib.core.javacv.util.FramesPerSecond;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.ViewPoint;
import teaselib.video.ResolutionList;
import teaselib.video.VideoCaptureDevice;
import teaselib.video.VideoCaptureDevices;

/**
 * @author Citizen-Cane
 * 
 *         Bullet-Proof motion detector:
 *         <p>
 *         Motion is detected via motion contours from background subtraction.
 *         Sensitivity is implemented by applying a structuring element to the
 *         motion contours.
 *         <p>
 *         Absence of motion is measured by measuring the distance of tracking
 *         points from positions set when motion last stopped. Motion is
 *         detected when the tracking points move too far away from their start
 *         points.
 *         <p>
 *         Blinking eyes are detected by removing small circular motion contours
 *         before calculating the motion region.
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
public class MotionDetectorJavaCV implements MotionDetector {
    private static final Logger logger = LoggerFactory
            .getLogger(MotionDetectorJavaCV.class);

    private static final String DeviceClassName = "MotionDetectorJavaCV";

    public static final DeviceFactory<MotionDetector> Factory = new DeviceFactory<MotionDetector>(
            DeviceClassName) {
        @Override
        public List<String> enumerateDevicePaths(
                Map<String, MotionDetector> deviceCache) {
            List<String> deviceNames = new ArrayList<String>();
            Set<String> videoCaptureDevicePaths = VideoCaptureDevices.Instance
                    .getDevicePaths();
            for (String videoCaptureDevicePath : videoCaptureDevicePaths) {
                deviceNames.add(DeviceCache.createDevicePath(DeviceClassName,
                        videoCaptureDevicePath));
            }
            return deviceNames;
        }

        @Override
        public MotionDetector createDevice(String deviceName) {
            return new MotionDetectorJavaCV(
                    VideoCaptureDevices.Instance.getDevice(deviceName));
        }

    };

    public static final EnumSet<Feature> Features = EnumSet.of(Feature.Motion,
            Feature.Presence);

    private static final double DesiredFps = 15;

    private final CaptureThread eventThread;

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        super();
        eventThread = new CaptureThread(videoCaptureDevice, DesiredFps);
        setSensitivity(MotionSensitivity.Normal);
        setViewPoint(ViewPoint.EyeLevel);
        Thread detectionEventsShutdownHook = new Thread() {
            @Override
            public void run() {
                close();
            }
        };
        Runtime.getRuntime().addShutdownHook(detectionEventsShutdownHook);
        eventThread.setDaemon(false);
        eventThread.setName(getDevicePath());
        eventThread.start();
    }

    static class CaptureThread extends Thread {
        private static final Size DesiredProcessingSize = new Size(320, 240);
        private static final boolean mirror = true;

        private static final Map<MotionSensitivity, Integer> motionSensitivities = new HashMap<MotionSensitivity, Integer>(
                initStructuringElementSizes());

        private VideoCaptureDevice videoCaptureDevice;

        private Transformation videoInputTransformation;
        private Size processingSize;
        private MotionSensitivity motionSensitivity;
        private ViewPoint viewPoint;
        private MotionProcessorJavaCV motionProcessor;
        private Mat input = new Mat();
        // TODO Atomic reference
        private MotionDetectionResultImplementation detectionResult;

        private final double desiredFps;
        private double fps;
        private FramesPerSecond fpsStatistics;
        private long desiredFrameTimeMillis;

        volatile double debugWindowTimeSpan = PresenceRegionDefaultTimespan;
        private final Lock lockStartStop = new ReentrantLock();
        private final Signal presenceChanged = new Signal();

        MotionDetectorJavaCVDebugRenderer debugInfo = null;
        boolean logDetails = false;

        CaptureThread(VideoCaptureDevice videoCaptureDevice,
                double desiredFps) {
            super();
            this.desiredFps = desiredFps;
            this.videoCaptureDevice = videoCaptureDevice;
        }

        private void openVideoCaptureDevice(
                VideoCaptureDevice videoCaptureDevice) {
            videoCaptureDevice.open();
            videoCaptureDevice.fps(fps);
            videoCaptureDevice.resolution(videoCaptureDevice.getResolutions()
                    .getMatchingOrSimilar(DesiredProcessingSize));
            this.fps = FramesPerSecond.getFps(videoCaptureDevice.fps(),
                    desiredFps);
            this.fpsStatistics = new FramesPerSecond((int) fps);
            this.desiredFrameTimeMillis = (long) (1000.0 / fps);
            Size resolution = videoCaptureDevice.resolution();
            if (mirror) {
                ScaleAndMirror scaleAndMirror = new ScaleAndMirror(resolution,
                        ResolutionList.getSmallestFit(resolution,
                                DesiredProcessingSize),
                        input);
                this.processingSize = new Size(
                        (int) (resolution.width() * scaleAndMirror.factor),
                        (int) (resolution.height() * scaleAndMirror.factor));
                this.videoInputTransformation = scaleAndMirror;
            } else if (resolution.equals(DesiredProcessingSize)) {
                // Copy source mat because when the video capture device
                // frame rate drops below the desired frame rate,
                // the debug renderer would mess up motion detection
                // when rendering into the source mat - at least for
                // javacv
                this.videoInputTransformation = new Copy(input);
                this.processingSize = resolution;
            } else {
                Scale scale = new Scale(resolution, DesiredProcessingSize,
                        input);
                this.processingSize = new Size(
                        (int) (resolution.width() * scale.factor),
                        (int) (resolution.height() * scale.factor));
                this.videoInputTransformation = scale;
            }
            motionProcessor = new MotionProcessorJavaCV(resolution,
                    processingSize);
            detectionResult = new MotionDetectionResultImplementation(
                    processingSize);
            setSensitivity(motionSensitivity);
            setPointOfView(viewPoint);
            debugInfo = new MotionDetectorJavaCVDebugRenderer(motionProcessor,
                    processingSize);
        }

        private static Map<MotionSensitivity, Integer> initStructuringElementSizes() {
            Map<MotionSensitivity, Integer> map = new HashMap<MotionSensitivity, Integer>();
            map.put(MotionSensitivity.High, 12);
            map.put(MotionSensitivity.Normal, 24);
            map.put(MotionSensitivity.Low, 36);
            return map;
        }

        public void setSensitivity(MotionSensitivity motionSensivity) {
            this.motionSensitivity = motionSensivity;
            if (motionProcessor != null) {
                motionProcessor.setStructuringElementSize(
                        motionSensitivities.get(motionSensivity));
            }
        }

        public void setPointOfView(ViewPoint viewPoint) {
            this.viewPoint = viewPoint;
            if (detectionResult != null) {
                detectionResult.setViewPoint(viewPoint);
            }
        }

        @Override
        public void run() {
            try {
                // TODO Auto-adjust until frame rate is stable
                // - KNN/findContours uses less cpu without motion
                // -> adjust to < 50% processing time per frame
                while (!isInterrupted()) {
                    // poll the device until its reconnected
                    DeviceCache.connect(videoCaptureDevice, -1);
                    openVideoCaptureDevice(videoCaptureDevice);
                    fpsStatistics.start();
                    try {
                        for (Mat frame : videoCaptureDevice) {
                            videoInputTransformation.update(frame);
                            try {
                                lockStartStop.lockInterruptibly();
                                // update shared items
                                motionProcessor.update(input);
                            } finally {
                                lockStartStop.unlock();
                            }
                            // Resulting bounding boxes
                            final long now = System.currentTimeMillis();
                            presenceChanged.doLocked(new Runnable() {
                                @Override
                                public void run() {
                                    boolean hasChanged = detectionResult
                                            .updateMotionState(input,
                                                    motionProcessor, now);
                                    if (hasChanged) {
                                        presenceChanged.signal();
                                    }
                                    updateDisplay(input, debugInfo);
                                }
                            });
                            long timeLeft = fpsStatistics.timeMillisLeft(
                                    desiredFrameTimeMillis, now);
                            if (timeLeft > 0) {
                                Thread.sleep(timeLeft);
                            }
                            fpsStatistics.updateFrame(now + timeLeft);
                        }
                    } catch (InterruptedException e) {
                        throw e;
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    } finally {
                        videoCaptureDevice.close();
                        if (debugInfo != null)
                            debugInfo.close();
                    }
                }
            } catch (InterruptedException e) {
                // Thread ends gracefully
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        private void updateDisplay(Mat image,
                MotionDetectorJavaCVDebugRenderer debugInfo) {
            Set<Presence> indicators = getIndicatorHistory(debugWindowTimeSpan);
            debugInfo.render(image,
                    detectionResult.getPresenceRegion(debugWindowTimeSpan),
                    detectionResult.presenceIndicators, indicators,
                    detectionResult.contourMotionDetected,
                    detectionResult.trackerMotionDetected,
                    fpsStatistics.value());
            if (logDetails) {
                logMotionState(indicators, motionProcessor,
                        detectionResult.contourMotionDetected,
                        detectionResult.trackerMotionDetected);
            }
        }

        private Set<Presence> getIndicatorHistory(double timeSpan) {
            List<Set<Presence>> indicatorHistory = detectionResult.indicatorHistory
                    .getTimeSpan(timeSpan);
            LinkedHashSet<Presence> indicators = new LinkedHashSet<Presence>();
            for (Set<Presence> set : indicatorHistory) {
                for (Presence item : set) {
                    // Add non-existing elements only
                    // if not there already to keep the sequence
                    // addAll wouldn't keep the sequence, because
                    // it would add existing elements at the end
                    if (!indicators.contains(item)) {
                        indicators.add(item);
                    }
                }
            }
            // remove negated indicators in the result set
            for (Map.Entry<Presence, Presence> entry : detectionResult.negatedRegions
                    .entrySet()) {
                if (indicators.contains(entry.getKey())
                        && indicators.contains(entry.getValue())) {
                    indicators.remove(entry.getValue());
                }
            }
            return indicators;
        }

        private static void logMotionState(Set<Presence> indicators,
                MotionProcessorJavaCV motionProcessor,
                boolean contourMotionDetected, boolean trackerMotionDetected) {
            // Log state
            logger.info("contourMotionDetected=" + contourMotionDetected
                    + "  trackerMotionDetected=" + trackerMotionDetected
                    + "(distance="
                    + motionProcessor.distanceTracker.distance2(
                            motionProcessor.trackFeatures.keyPoints())
                    + "), " + indicators.toString());
        }

        public void clearMotionHistory() {
            presenceChanged.doLocked(new Runnable() {
                @Override
                public void run() {
                    detectionResult.clear();
                }
            });
        }

        public double fps() {
            return fps;
        }

    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                eventThread.videoCaptureDevice.getDevicePath());
    }

    @Override
    public String getName() {
        return "Motion Detector";
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensivity) {
        pause();
        eventThread.setSensitivity(motionSensivity);
        resume();
    }

    @Override
    public void setViewPoint(ViewPoint pointOfView) {
        pause();
        eventThread.setPointOfView(pointOfView);
        resume();
    }

    @Override
    public EnumSet<Feature> getFeatures() {
        return Features;
    }

    @Override
    public boolean awaitChange(double amount, Presence change,
            double timeSpanSeconds, double timeoutSeconds) {
        if (!active()) {
            try {
                Thread.sleep((long) (timeoutSeconds * 1000));
            } catch (InterruptedException e) {
                throw new ScriptInterruptedException();
            }
            return false;
        }
        eventThread.debugWindowTimeSpan = timeSpanSeconds;
        try {
            return eventThread.detectionResult.awaitChange(
                    eventThread.presenceChanged, amount, change,
                    timeSpanSeconds, timeoutSeconds);
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } finally {
            eventThread.debugWindowTimeSpan = MotionRegionDefaultTimespan;
        }
    }

    protected double fps() {
        return eventThread.fps();
    }

    @Override
    public void clearMotionHistory() {
        eventThread.clearMotionHistory();
    }

    @Override
    public boolean connected() {
        return eventThread.videoCaptureDevice.connected();
    }

    @Override
    public boolean active() {
        return eventThread.detectionResult != null
                && eventThread.videoCaptureDevice.active();
    }

    @Override
    public void pause() {
        eventThread.lockStartStop.lock();
    }

    @Override
    public void resume() {
        eventThread.lockStartStop.unlock();
    }

    @Override
    public void close() {
        eventThread.interrupt();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
