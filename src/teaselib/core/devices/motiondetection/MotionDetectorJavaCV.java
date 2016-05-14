package teaselib.core.devices.motiondetection;

import static teaselib.core.javacv.util.Geom.intersects;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.TeaseLib;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.Signal;
import teaselib.core.devices.DeviceCache;
import teaselib.core.javacv.util.FramesPerSecond;
import teaselib.core.javacv.util.Geom;
import teaselib.core.util.TimeLine;
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
public class MotionDetectorJavaCV extends BasicMotionDetector {
    public static final String DeviceClassName = "MotionDetectorJavaCV";
    private static final int DesiredFps = 15;

    public static final EnumSet<Feature> Features = EnumSet.of(Feature.Motion,
            Feature.Presence);
    private static final Map<MotionSensitivity, Integer> motionSensitivities = new HashMap<>(
            initStructuringElementSizes());

    private static Map<MotionSensitivity, Integer> initStructuringElementSizes() {
        Map<MotionSensitivity, Integer> map = new HashMap<>();
        map.put(MotionSensitivity.High, 12);
        map.put(MotionSensitivity.Normal, 24);
        map.put(MotionSensitivity.Low, 36);
        return map;
    }

    protected DetectionEventsJavaCV detectionEvents;
    Signal presenceChanged = new Signal();

    private final double desiredFPS;
    int distanceThreshold2;

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        super();
        desiredFPS = FramesPerSecond.getFps(videoCaptureDevice.fps(),
                DesiredFps);
        detectionEvents = new DetectionEventsJavaCV(videoCaptureDevice);
        setSensitivity(MotionSensitivity.Normal);
        Thread detectionEventsShutdownHook = new Thread() {
            @Override
            public void run() {
                release();
            }
        };
        Runtime.getRuntime().addShutdownHook(detectionEventsShutdownHook);
        detectionEvents.setDaemon(false);
        detectionEvents.setName(getDevicePath());
        detectionEvents.start();
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                detectionEvents.videoCaptureDevice.getDevicePath());
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensivity) {
        pause();
        MotionProcessorJavaCV motionDetector = detectionEvents.motionProcessor;
        motionDetector.setStructuringElementSize(
                motionSensitivities.get(motionSensivity));
        int s = motionDetector.structuringElementSize;
        distanceThreshold2 = s * s;
        resume();
    }

    @Override
    public EnumSet<Feature> getFeatures() {
        return Features;
    }

    @Override
    // TODO forward to detection events
    public boolean isMotionDetected(double seconds) {
        TimeLine<Integer> motionAreaHistory = detectionEvents.motionAreaHistory;
        synchronized (motionAreaHistory) {
            Statistics statistics = new Statistics(
                    motionAreaHistory.getTimeSpan(seconds));
            double motion = statistics.max();
            return motion > 0.0;
        }
    }

    @Override
    public EnumSet<Presence> getPresence() {
        final EnumSet<Presence> presence = EnumSet.noneOf(Presence.class);
        try {
            presenceChanged.doLocked(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    presence.addAll(detectionEvents.indicatorHistory.tail());
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
                            final TimeLine<Set<Presence>> indicatorHistory = detectionEvents.indicatorHistory;
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

    private class DetectionEventsJavaCV extends DetectionEvents {
        private static final int cornerSize = 32;
        private static final double MotionRegionJoinTimespan = 1.0;
        private static final double PresenceRegionJoinTimespan = 1.0;
        // TODO exactly define Circularity and its calculation
        private static final double CircularityVariance = 1.3;   // 1.3 seems to be necessary to detect blinking eye balls

        private final VideoCaptureDevice videoCaptureDevice;
        private final MotionProcessorJavaCV motionProcessor;
        private final Map<Presence, Rect> presenceIndicators;
        private int motionDetectedCounter = -1;

        long desiredFrameTimeMillis = (long) (1000.0 / desiredFPS);
        FramesPerSecond fps = new FramesPerSecond((int) desiredFPS);
        private Rect motionRegion = null;
        private Rect presenceRegion = null;

        TimeLine<Rect> motionRegionHistory = new TimeLine<Rect>();
        TimeLine<Rect> presenceRegionHistory = new TimeLine<Rect>();
        TimeLine<Integer> motionAreaHistory = new TimeLine<Integer>();
        TimeLine<Set<Presence>> indicatorHistory = new TimeLine<Set<Presence>>();

        private boolean contourMotionDetected = false;
        private boolean trackerMotionDetected = false;
        private boolean motionDetected = false;

        boolean debug = true;
        MotionDetectorJavaCVDebugRenderer debugInfo = null;
        boolean logDetails = false;

        DetectionEventsJavaCV(VideoCaptureDevice videoCaptureDevice) {
            super();
            this.videoCaptureDevice = videoCaptureDevice;
            final Size size = new Size(320, 240);
            videoCaptureDevice.open(size);
            Size actualSize = videoCaptureDevice.size();
            motionProcessor = new MotionProcessorJavaCV(
                    videoCaptureDevice.captureSize().width(),
                    actualSize.width());
            presenceIndicators = buildPresenceIndicatorMap(actualSize);
            initHistoryLists();
        }

        void initHistoryLists() {
            motionRegionHistory.clear();
            presenceRegionHistory.clear();
            motionAreaHistory.clear();
            indicatorHistory.clear();
            // Set start values,
            // instead of testing for empty lists later on
            Size size = videoCaptureDevice.size();
            long timeStamp = 0;
            Rect all = new Rect(0, 0, size.width(), size.height());
            motionRegionHistory.add(all, timeStamp);
            presenceRegionHistory.add(all, timeStamp);
            motionAreaHistory.add(0, timeStamp);
            indicatorHistory.add(getPresence(all, all), timeStamp);

        }

        private Map<Presence, Rect> buildPresenceIndicatorMap(Size s) {
            Map<Presence, Rect> map = new HashMap<>();
            map.put(Presence.Present, new Rect(cornerSize, cornerSize,
                    s.width() - 2 * cornerSize, s.height() - 2 * cornerSize));
            // Borders
            map.put(Presence.LeftBorder,
                    new Rect(0, 0, cornerSize, s.height()));
            map.put(Presence.RightBorder, new Rect(s.width() - cornerSize, 0,
                    cornerSize, s.height()));
            map.put(Presence.TopBorder, new Rect(0, 0, s.width(), cornerSize));
            map.put(Presence.BottomBorder, new Rect(0, s.height() - cornerSize,
                    s.width(), cornerSize));
            // Define a center rectangle half the width
            // and third the height of the capture size
            int cl = s.width() / 2 - s.width() / 4;
            int ct = s.height() / 2 - s.width() / 6;
            int cr = s.width() / 2 + s.width() / 4;
            int cb = s.height() / 2 + s.width() / 6;
            // Center
            map.put(Presence.Center, new Rect(cl, ct, cr - cl, cb - ct));
            map.put(Presence.CenterHorizontal,
                    new Rect(0, ct, s.width(), cb - ct));
            map.put(Presence.CenterVertical,
                    new Rect(cl, 0, cr - cl, s.height()));
            // Sides
            map.put(Presence.Left, new Rect(0, 0, cl, s.height()));
            map.put(Presence.Top, new Rect(0, 0, s.width(), ct));
            map.put(Presence.Right,
                    new Rect(cr, 0, s.width() - cr, s.height()));
            map.put(Presence.Bottom,
                    new Rect(0, cb, s.width(), s.height() - cb));
            return map;
        }

        private EnumSet<Presence> getPresence(Rect motionRegion,
                Rect presenceRegion) {
            boolean motionDetected = isMotionDetected(MotionRegionJoinTimespan);
            Rect presenceRect = presenceIndicators.get(Presence.Present);
            if (motionRegion == null
                    || (motionRegion.contains(presenceRect.tl())
                            && motionRegion.contains(presenceRect.br()))) {
                // TODO keep last state, to minimize wrong application behavior
                // caused by small shakes
                return EnumSet.of(Presence.Shake);
            } else {
                boolean presenceInsidePresenceRect = intersects(presenceRegion,
                        presenceRect);
                Presence presenceState = motionDetected
                        || presenceInsidePresenceRect ? Presence.Present
                                : Presence.Away;
                Set<Presence> directions = new HashSet<>();
                for (Map.Entry<Presence, Rect> e : presenceIndicators
                        .entrySet()) {
                    if (e.getKey() != Presence.Present) {
                        if (intersects(e.getValue(), motionRegion)) {
                            directions.add(e.getKey());
                        }
                    }
                }
                if (motionDetected) {
                    directions.add(Presence.Motion);
                } else {
                    directions.add(Presence.NoMotion);
                }
                Presence[] directionsArray = new Presence[directions.size()];
                directionsArray = directions.toArray(directionsArray);
                return EnumSet.of(presenceState, directionsArray);
            }
        }

        boolean isMotionDetected(double seconds) {
            Statistics statistics = new Statistics(
                    motionAreaHistory.getTimeSpan(seconds));
            double motion = statistics.max();
            return motion > 0.0;
        }

        @Override
        public void run() {
            // TODO Auto-adjust until frame rate is stable
            // - KNN/findContours use less cpu without motion
            // -> adjust to < 50% processing time per frame
            motionDetectedCounter = -1;
            fps.startFrame();
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
                            updateMotionState(videoImage, now);
                            return true;
                        }
                    });
                } catch (Exception e) {
                    TeaseLib.instance().log.error(this, e);
                }
                long timeLeft = fps.timeMillisLeft(desiredFrameTimeMillis, now);
                if (timeLeft > 0) {
                    try {
                        Thread.sleep(timeLeft);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                fps.updateFrame(now + timeLeft);
            }
            videoCaptureDevice.release();
        }

        private void updateMotionState(Mat videoImage, long timeStamp) {
            updateMotionAndPresence(videoImage, timeStamp);
            updateMotionTimeLine(timeStamp);
            updateIndicatorTimeLine(timeStamp);
            Set<Presence> indicators = indicatorHistory.last(1).get(0);
            debugInfo.render(videoImage, presenceRegion, presenceIndicators,
                    indicators, contourMotionDetected, trackerMotionDetected,
                    fps.value());
            if (logDetails) {
                logMotionState(indicators, contourMotionDetected,
                        trackerMotionDetected);
            }
        }

        private void updateMotionAndPresence(Mat videoImage, long timeStamp) {
            // Motion history
            // TODO filter out Shakes (actual shakes, light changes)
            // TODO cluster motion an presence regions by time
            // so that we don't have to use a fixed time span
            List<Rect> presenceRegions = motionProcessor.motionContours
                    .regions();
            if (presenceRegions.isEmpty()) {
                // Even just adding the time stamp
                // makes our motion region fade away
                presenceRegionHistory.add(timeStamp);
            } else {
                presenceRegionHistory.add(Geom.join(presenceRegions),
                        timeStamp);
            }
            // moving forward in time changes the presence region, so
            // eventually it collapses on the current presence region
            // however we just want presence, no more,
            // and blinking eyes in the border regions
            // should count as "Away" anyway
            // -> no memory for presence needed
            presenceRegion = Geom.join(presenceRegionHistory
                    .getTimeSpan(PresenceRegionJoinTimespan));
            // Remove potential blinking eyes from motion region history
            List<Rect> motionRegions = new Vector<Rect>();
            MatVector contours = motionProcessor.motionContours.contours;
            for (int i = 0; i < presenceRegions.size(); i++) {
                if (!Geom.isCircular(contours.get(i), CircularityVariance)) {
                    motionRegions.add(presenceRegions.get(i));
                }
            }
            if (motionRegions.isEmpty()) {
                // Even just adding the time stamp
                // makes our motion region fade away
                motionRegionHistory.add(timeStamp);
                //
            } else {
                motionRegionHistory.add(Geom.join(motionRegions), timeStamp);
            }
            // moving forward in time changes the motion region, so
            // eventually the motion region collapses on the last motion region
            motionRegion = Geom.join(
                    motionRegionHistory.getTimeSpan(MotionRegionJoinTimespan));
            // Contour motion
            contourMotionDetected = motionRegions.size() > 0;
            // Tracker motion
            motionProcessor.distanceTracker.update(videoImage,
                    contourMotionDetected, motionProcessor.trackFeatures);
            double distance2 = motionProcessor.distanceTracker
                    .distance2(motionProcessor.trackFeatures.keyPoints());
            trackerMotionDetected = distance2 > distanceThreshold2;
            // All together combined
            motionDetected = contourMotionDetected || trackerMotionDetected;
        }

        private void updateMotionTimeLine(long timeStamp) {
            final int motionArea;
            if (motionDetected) {
                motionArea = motionRegionHistory.tail().area();
            } else {
                motionArea = 0;
            }
            synchronized (motionAreaHistory) {
                motionAreaHistory.add(motionArea, timeStamp);
            }
        }

        private void updateIndicatorTimeLine(long timeStamp) {
            Set<Presence> indicators = getPresence(motionRegion,
                    presenceRegion);
            boolean hasChanged = indicatorHistory.add(indicators, timeStamp);
            if (hasChanged) {
                presenceChanged.signal();
            }
        }

        private void logMotionState(Set<Presence> indicators,
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
    }

    @Override
    protected double fps() {
        return desiredFPS;
    }

    @Override
    public void clearMotionHistory() {
        detectionEvents.initHistoryLists();
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
        return detectionEvents != null;
    }

    @Override
    public void pause() {
        detectionEvents.lockStartStop.lock();
    }

    @Override
    public void resume() {
        detectionEvents.lockStartStop.unlock();
    }

    @Override
    public void release() {
        detectionEvents.interrupt();
        try {
            detectionEvents.join();
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
