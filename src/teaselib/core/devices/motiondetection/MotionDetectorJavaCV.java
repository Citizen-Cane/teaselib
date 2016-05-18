package teaselib.core.devices.motiondetection;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.TeaseLib;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.Signal;
import teaselib.core.devices.DeviceCache;
import teaselib.core.javacv.util.FramesPerSecond;
import teaselib.core.util.TimeLine;
import teaselib.motiondetection.MotionDetector;
import teaselib.util.math.Statistics;
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

    private static final int DesiredFps = 15;
    private static final Map<MotionSensitivity, Integer> motionSensitivities = new HashMap<>(
            initStructuringElementSizes());

    private static Map<MotionSensitivity, Integer> initStructuringElementSizes() {
        Map<MotionSensitivity, Integer> map = new HashMap<>();
        map.put(MotionSensitivity.High, 12);
        map.put(MotionSensitivity.Normal, 24);
        map.put(MotionSensitivity.Low, 36);
        return map;
    }

    private VideoCaptureThread videoCaptureThread;
    private final Signal presenceChanged = new Signal();

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        super();
        videoCaptureThread = new VideoCaptureThread(videoCaptureDevice);
        setSensitivity(MotionSensitivity.Normal);
        Thread detectionEventsShutdownHook = new Thread() {
            @Override
            public void run() {
                release();
            }
        };
        Runtime.getRuntime().addShutdownHook(detectionEventsShutdownHook);
        videoCaptureThread.setDaemon(false);
        videoCaptureThread.setName(getDevicePath());
        videoCaptureThread.start();
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                videoCaptureThread.videoCaptureDevice.getDevicePath());
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensivity) {
        pause();
        MotionProcessorJavaCV motionDetector = videoCaptureThread.motionProcessor;
        motionDetector.setStructuringElementSize(
                motionSensitivities.get(motionSensivity));
        resume();
    }

    @Override
    public EnumSet<Feature> getFeatures() {
        return Features;
    }

    @Override
    public boolean isMotionDetected(final double seconds) {
        final List<Integer> values = new Vector<Integer>();
        try {
            presenceChanged.doLocked(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    values.addAll(
                            videoCaptureThread.detectionResults.motionAreaHistory
                                    .getTimeSpan(seconds));
                    return true;
                }
            });
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
        Statistics statistics = new Statistics(values);
        double motion = statistics.max();
        return motion > 0.0;
    }

    @Override
    public Set<Presence> getPresence() {
        final EnumSet<Presence> presence = EnumSet.noneOf(Presence.class);
        try {
            presenceChanged.doLocked(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    presence.addAll(
                            videoCaptureThread.detectionResults.indicatorHistory
                                    .tail());
                    return true;
                }
            });
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
        return presence;
    }

    @Override
    public boolean awaitChange(final double timeoutSeconds,
            final Presence change) {
        try {
            return presenceChanged.awaitChange(timeoutSeconds,
                    (new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            final TimeLine<Set<Presence>> indicatorHistory = videoCaptureThread.detectionResults.indicatorHistory;
                            Set<Presence> current = indicatorHistory.tail();
                            return current.contains(change);
                        }
                    }));
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
        return false;
    }

    private class VideoCaptureThread extends Thread {

        private final VideoCaptureDevice videoCaptureDevice;
        private final MotionProcessorJavaCV motionProcessor;

        final double fps;
        private final FramesPerSecond fpsStatistics;
        private final long desiredFrameTimeMillis;

        final MotionDetectionResults detectionResults;

        MotionDetectorJavaCVDebugRenderer debugInfo = null;
        boolean logDetails = false;

        public final Lock lockStartStop = new ReentrantLock();

        VideoCaptureThread(VideoCaptureDevice videoCaptureDevice) {
            super();
            this.videoCaptureDevice = videoCaptureDevice;
            this.fps = FramesPerSecond.getFps(videoCaptureDevice.fps(),
                    DesiredFps);
            this.fpsStatistics = new FramesPerSecond((int) fps);
            this.desiredFrameTimeMillis = (long) (1000.0 / fps);

            final Size size = new Size(320, 240);
            videoCaptureDevice.open(size);
            Size actualSize = videoCaptureDevice.size();
            motionProcessor = new MotionProcessorJavaCV(
                    videoCaptureDevice.captureSize().width(),
                    actualSize.width());
            detectionResults = new MotionDetectionResults(actualSize);

        }

        @Override
        public void run() {
            // TODO Auto-adjust until frame rate is stable
            // - KNN/findContours use less cpu without motion
            // -> adjust to < 50% processing time per frame
            fpsStatistics.startFrame();
            debugInfo = new MotionDetectorJavaCVDebugRenderer(motionProcessor,
                    videoCaptureDevice.size());
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
                try {
                    presenceChanged.doLocked(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            boolean hasChanged = detectionResults
                                    .updateMotionState(videoImage,
                                            motionProcessor, now);
                            if (hasChanged) {
                                presenceChanged.signal();
                            }
                            updateWindow(videoImage, debugInfo);
                            return true;
                        }
                    });
                } catch (Exception e) {
                    TeaseLib.instance().log.error(this, e);
                }
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
            videoCaptureDevice.release();
        }

        private void updateWindow(Mat videoImage,
                MotionDetectorJavaCVDebugRenderer debugInfo) {
            Set<Presence> indicators = detectionResults.indicatorHistory.last(1)
                    .get(0);
            debugInfo.render(videoImage, detectionResults.presenceRegion,
                    detectionResults.presenceIndicators, indicators,
                    detectionResults.contourMotionDetected,
                    detectionResults.trackerMotionDetected,
                    fpsStatistics.value());
            if (logDetails) {
                logMotionState(indicators, motionProcessor,
                        detectionResults.contourMotionDetected,
                        detectionResults.trackerMotionDetected);
            }
        }

        private void logMotionState(Set<Presence> indicators,
                MotionProcessorJavaCV motionProcessor,
                boolean contourMotionDetected, boolean trackerMotionDetected) {
            // Log state
            TeaseLib.instance().log.info(indicators.toString());
            TeaseLib.instance().log
                    .info("contourMotionDetected=" + contourMotionDetected
                            + "  trackerMotionDetected=" + trackerMotionDetected
                            + "(distance="
                            + motionProcessor.distanceTracker.distance2(
                                    motionProcessor.trackFeatures.keyPoints())
                    + ")");
        }

        public void clearMotionHistory() {
            detectionResults.clear(videoCaptureDevice.size());
        }
    }

    protected double fps() {
        return videoCaptureThread.fps;
    }

    @Override
    public void clearMotionHistory() {
        try {
            presenceChanged.doLocked(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    videoCaptureThread.clearMotionHistory();
                    return true;
                }
            });
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
    }

    @Override
    public boolean awaitMotionStart(double timeoutSeconds) {
        return awaitChange(timeoutSeconds, Presence.Motion);
    }

    @Override
    public boolean awaitMotionEnd(double timeoutSeconds) {
        return awaitChange(timeoutSeconds, Presence.NoMotion);
    }

    @Override
    public boolean active() {
        return videoCaptureThread != null;
    }

    @Override
    public void pause() {
        videoCaptureThread.lockStartStop.lock();
    }

    @Override
    public void resume() {
        videoCaptureThread.lockStartStop.unlock();
    }

    @Override
    public void release() {
        videoCaptureThread.interrupt();
        try {
            videoCaptureThread.join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
