package teaselib.core.devices.motiondetection;

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

import teaselib.TeaseLib;
import teaselib.core.concurrency.Signal;
import teaselib.core.devices.DeviceCache;
import teaselib.core.javacv.util.FramesPerSecond;
import teaselib.motiondetection.MotionDetection;
import teaselib.motiondetection.MotionDetector;
import teaselib.video.VideoCaptureDevice;

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
 *         TODO Motion start end end are measure how? Immediately or with delay.
 *         Which?
 *         <p>
 *         Blinking eyes are detected by removing small circular motion contours
 *         before calculating the motion region.
 *         <p>
 *         The detector is stable, but still can be cheated/misled. Avoid:
 *         <p>
 *         pets <b> audience within the field of view
 *         <p>
 *         light sources from moving cars or traffic lights
 */
public class MotionDetectorJavaCV implements MotionDetector {
    public static final String DeviceClassName = "MotionDetectorJavaCV";

    public static final EnumSet<Feature> Features = EnumSet.of(Feature.Motion,
            Feature.Presence);

    private static final double DesiredFps = 15;

    private final CaptureThread eventThread;

    private static final Map<MotionSensitivity, Integer> motionSensitivities = new HashMap<MotionSensitivity, Integer>(
            initStructuringElementSizes());

    private static Map<MotionSensitivity, Integer> initStructuringElementSizes() {
        Map<MotionSensitivity, Integer> map = new HashMap<MotionSensitivity, Integer>();
        map.put(MotionSensitivity.High, 12);
        map.put(MotionSensitivity.Normal, 24);
        map.put(MotionSensitivity.Low, 36);
        return map;
    }

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        super();
        eventThread = new CaptureThread(videoCaptureDevice, DesiredFps);
        setSensitivity(MotionSensitivity.Normal);
        Thread detectionEventsShutdownHook = new Thread() {
            @Override
            public void run() {
                release();
            }
        };
        Runtime.getRuntime().addShutdownHook(detectionEventsShutdownHook);
        eventThread.setDaemon(false);
        eventThread.setName(getDevicePath());
        eventThread.start();
    }

    static class CaptureThread extends Thread {
        private static final Size DesiredProcessingSize = new Size(320, 240);

        private final VideoCaptureDevice videoCaptureDevice;
        private final MotionProcessorJavaCV motionProcessor;
        final MotionDetectionResultImplementation detectionResult;
        private final double fps;
        private final FramesPerSecond fpsStatistics;
        private final long desiredFrameTimeMillis;

        volatile double debugWindowTimeSpan = PresenceRegionDefaultTimespan;
        final Lock lockStartStop = new ReentrantLock();
        final Signal presenceChanged = new Signal();

        MotionDetectorJavaCVDebugRenderer debugInfo = null;
        boolean logDetails = false;

        CaptureThread(VideoCaptureDevice videoCaptureDevice,
                double desiredFps) {
            super();
            this.videoCaptureDevice = videoCaptureDevice;
            this.fps = FramesPerSecond.getFps(videoCaptureDevice.fps(),
                    desiredFps);
            this.fpsStatistics = new FramesPerSecond((int) fps);
            this.desiredFrameTimeMillis = (long) (1000.0 / fps);

            videoCaptureDevice.open(DesiredProcessingSize);
            @SuppressWarnings("resource")
            Size actualSize = videoCaptureDevice.size();
            motionProcessor = new MotionProcessorJavaCV(
                    videoCaptureDevice.captureSize().width(),
                    actualSize.width());
            detectionResult = new MotionDetectionResultImplementation(
                    actualSize);

        }

        @Override
        public void run() {
            try {
                // TODO Auto-adjust until frame rate is stable
                // - KNN/findContours uses less cpu without motion
                // -> adjust to < 50% processing time per frame
                fpsStatistics.startFrame();
                debugInfo = new MotionDetectorJavaCVDebugRenderer(
                        motionProcessor, videoCaptureDevice.size());
                for (final Mat videoImage : videoCaptureDevice) {
                    // TODO handle camera surprise removal and reconnect
                    // -> in VideoCaptureDevice?
                    if (isInterrupted()) {
                        break;
                    }
                    // handle setting sensitivity via structuring element size
                    // TODO it's just setting a member, maybe we can ignore
                    // concurrency
                    try {
                        lockStartStop.lockInterruptibly();
                        // update shared items
                        motionProcessor.update(videoImage);
                    } catch (InterruptedException e1) {
                        break;
                    } finally {
                        lockStartStop.unlock();
                    }
                    // Resulting bounding boxes
                    final long now = System.currentTimeMillis();
                    presenceChanged.doLocked(new Runnable() {
                        @Override
                        public void run() {
                            boolean hasChanged = detectionResult
                                    .updateMotionState(videoImage,
                                            motionProcessor, now);
                            if (hasChanged) {
                                presenceChanged.signal();
                            }
                            updateWindow(videoImage, debugInfo);
                        }
                    });
                    long timeLeft = fpsStatistics
                            .timeMillisLeft(desiredFrameTimeMillis, now);
                    if (timeLeft > 0) {
                        try {
                            Thread.sleep(timeLeft);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    fpsStatistics.updateFrame(now + timeLeft);
                }
            } catch (Exception e) {
                TeaseLib.instance().log.error(this, e);
            } finally {
                videoCaptureDevice.release();
            }
        }

        private void updateWindow(Mat videoImage,
                MotionDetectorJavaCVDebugRenderer debugInfo) {
            Set<Presence> indicators = getIndicatorHistory(debugWindowTimeSpan);
            debugInfo.render(videoImage,
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
            TeaseLib.instance().log.info("contourMotionDetected="
                    + contourMotionDetected + "  trackerMotionDetected="
                    + trackerMotionDetected + "(distance="
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
    public void setSensitivity(MotionSensitivity motionSensivity) {
        pause();
        eventThread.motionProcessor.setStructuringElementSize(
                motionSensitivities.get(motionSensivity));
        resume();
    }

    @Override
    public EnumSet<Feature> getFeatures() {
        return Features;
    }

    // @Override
    // public boolean awaitChange(final double timeoutSeconds,
    // final Presence change) {
    // return awaitChange(1.0, change, MotionRegionDefaultTimespan,
    // timeoutSeconds);
    // }

    @Override
    public boolean awaitChange(double amount, Presence change,
            double timeSpanSeconds, double timeoutSeconds) {
        eventThread.debugWindowTimeSpan = timeSpanSeconds;
        try {
            return eventThread.detectionResult.awaitChange(
                    eventThread.presenceChanged, amount, change,
                    timeSpanSeconds, timeoutSeconds);
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
    public boolean awaitMotionStart(double timeoutSeconds) {
        return MotionDetection.movement(this).startedWithin(timeoutSeconds);
    }

    @Override
    public boolean awaitMotionEnd(double timeoutSeconds) {
        return MotionDetection.movement(this).stoppedWithin(timeoutSeconds);
    }

    @Override
    public boolean active() {
        return eventThread.videoCaptureDevice.active();
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
    public void release() {
        eventThread.interrupt();
        try {
            eventThread.join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
