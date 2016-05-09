package teaselib.core.devices.motiondetection;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static teaselib.core.javacv.Color.*;
import static teaselib.core.javacv.util.Geom.*;
import static teaselib.core.javacv.util.Gui.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.core.javacv.util.FramesPerSecond;
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
 *         Absence of motion is detected by measuring the distance of tracking
 *         points from positions set when motion last stopped.
 *         <p>
 *         Motion start is immediately signaled, whereas motion end is signaled
 *         after a few frames delay in order to catch short breaks.
 *         <p>
 *         Blinking eyes are detected by removing small circular motion contours
 *         before calculating the motion region.
 */
public class MotionDetectorJavaCV extends BasicMotionDetector {
    public static final String DeviceClassName = "MotionDetectorJavaCV";

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

    private static final int MinimalFps = 15;
    private final VideoCaptureDevice videoCaptureDevice;
    private final int desiredFPS;
    int distanceThreshold2;

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        super();
        this.videoCaptureDevice = videoCaptureDevice;
        this.desiredFPS = getDesiredFPS(videoCaptureDevice, MinimalFps);
        detectionEvents = new DetectionEventsJavaCV();
        setSensitivity(MotionSensitivity.Normal);
        Thread detectionEventsShutdownHook = new Thread() {
            @Override
            public void run() {
                release();
            }
        };
        Runtime.getRuntime().addShutdownHook(detectionEventsShutdownHook);
        detectionEvents.setDaemon(false);
        detectionEvents.start();
    }

    // TODO fps is double
    private static int getDesiredFPS(VideoCaptureDevice videoCaptureDevice,
            int minimalFps) {
        final int desiredFPS;
        int vcfps = (int) videoCaptureDevice.fps();
        if (vcfps > minimalFps * 2) {
            int div = Math.floorDiv(vcfps, minimalFps * 2) + 1;
            desiredFPS = vcfps / div;
        } else if (vcfps > minimalFps) {
            desiredFPS = vcfps;
        } else {
            desiredFPS = minimalFps;
        }
        return desiredFPS;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                videoCaptureDevice.getDevicePath());
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensivity) {
        pause();
        MotionProcessorJavaCV motionDetector = ((DetectionEventsJavaCV) detectionEvents).motionDetector;
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
    protected boolean isMotionDetected(int pastFrames) {
        DetectionEventsJavaCV detectionEventsJavaCV = (DetectionEventsJavaCV) detectionEvents;
        final MotionAreaHistory motionAreaHistory = detectionEventsJavaCV.motionAreaHistory;
        synchronized (motionAreaHistory) {
            double dm = motionAreaHistory.getMotion(pastFrames);
            return dm > 0.0;
        }
    }

    @Override
    public EnumSet<Presence> getPresence() {
        // Presence / absence is checked by detecting movement in the border
        // regions without movement in the presence/center region
        // To make this work only the current movement region must be
        // considered, as the region history usually covers a larger area.
        // TODO Reality check with full body movement
        RegionHistory regionHistory = ((DetectionEventsJavaCV) detectionEvents).motionHistory;
        if (regionHistory.size() > 0) {
            Rect r = regionHistory.tail();
            return getPresence(r);
        } else {
            return EnumSet.noneOf(Presence.class);
        }
    }

    private EnumSet<Presence> getPresence(Rect r) {
        boolean motionDetected = isMotionDetected(1);
        Map<Presence, Rect> m = ((DetectionEventsJavaCV) detectionEvents).presenceIndicators;
        Rect presence = m.get(Presence.Present);
        if (r.contains(presence.tl()) && r.contains(presence.br())) {
            return EnumSet.of(Presence.Shake);
        } else {
            Presence presenceState = motionDetected
                    || intersects(r, m.get(Presence.Present)) ? Presence.Present
                            : Presence.Absent;
            Set<Presence> directions = new HashSet<>();
            for (Map.Entry<Presence, Rect> e : m.entrySet()) {
                if (e.getKey() != Presence.Present) {
                    if (intersects(e.getValue(), r)) {
                        directions.add(e.getKey());
                    }
                }
            }
            Presence[] directionsArray = new Presence[directions.size()];
            directionsArray = directions.toArray(directionsArray);
            return EnumSet.of(presenceState, directionsArray);
        }
    }

    private class DetectionEventsJavaCV extends DetectionEvents {
        private static final String INPUT = "Input";
        private static final String MOTION = "Motion";

        private static final int cornerSize = 32;

        private final MotionProcessorJavaCV motionDetector;
        private final Map<Presence, Rect> presenceIndicators;
        private int motionDetectedCounter = -1;

        long desiredFrameTime = 1000 / desiredFPS;
        FramesPerSecond fps = new FramesPerSecond(desiredFPS);
        private Rect motionRegion = MotionProcessorJavaCV.None;
        private Rect presenceRegion = MotionProcessorJavaCV.None;

        MotionDistanceTracker distanceTracker = new MotionDistanceTracker(
                Green);
        RegionHistory motionHistory = new RegionHistory(desiredFPS);
        RegionHistory presenceHistory = new RegionHistory(desiredFPS);
        MotionAreaHistory motionAreaHistory = new MotionAreaHistory(
                BasicMotionDetector.MaximumNumberOfPastFrames);

        private boolean contourMotionDetected = false;
        private boolean trackerMotionDetected = false;
        private boolean motionDetected = false;

        boolean debug = true;
        boolean logDetails = false;

        DetectionEventsJavaCV() {
            super();
            final Size size = new Size(320, 240);
            videoCaptureDevice.open(size);
            Size actualSize = videoCaptureDevice.size();
            motionDetector = new MotionProcessorJavaCV(
                    videoCaptureDevice.captureSize().width(),
                    actualSize.width());
            presenceIndicators = buildPresenceIndicatorMap(actualSize);
        }

        private Map<Presence, Rect> buildPresenceIndicatorMap(Size s) {
            Map<Presence, Rect> map = new HashMap<>();
            map.put(Presence.Present, new Rect(cornerSize, cornerSize,
                    s.width() - 2 * cornerSize, s.height() - 2 * cornerSize));
            map.put(Presence.Left, new Rect(0, 0, cornerSize, s.height()));
            map.put(Presence.Right, new Rect(s.width() - cornerSize, 0,
                    cornerSize, s.height()));
            map.put(Presence.Top, new Rect(0, 0, s.width(), cornerSize));
            map.put(Presence.Bottom, new Rect(0, s.height() - cornerSize,
                    s.width(), cornerSize));
            return map;
        }

        @Override
        public void run() {
            // TODO Auto-adjust until frame rate is stable
            // - KNN/findContours use less cpu without motion
            // -> adjust to < 50% processing time per frame
            if (debug) {
                String windows[] = { INPUT, MOTION };
                Size windowSize = videoCaptureDevice.size();
                positionWindows(windowSize.width(), windowSize.height(),
                        windows);
            }
            motionDetectedCounter = -1;
            fps.startFrame();
            for (final Mat videoImage : videoCaptureDevice) {
                // TODO handle camera surprise removal and reconnect
                // -> in VideoCaptureDevice?
                if (isInterrupted()) {
                    break;
                }
                try {
                    lockStartStop.lockInterruptibly();
                } catch (InterruptedException e1) {
                    break;
                }
                // update shared items
                detectionEvents.lockStartStop.lock();
                motionDetector.update(videoImage);
                lockStartStop.unlock();
                // Resulting bounding boxes
                updateMotionState(videoImage);
                long now = System.currentTimeMillis();
                long timeLeft = fps.timeMillisLeft(desiredFrameTime, now);
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

        private void updateMotionState(Mat videoImage) {
            updateMotionAndPresence();
            updateMotionArea();
            distanceTracker.update(videoImage, contourMotionDetected,
                    motionDetector.trackFeatures);
            renderDebugInfo(videoImage, motionRegion, contourMotionDetected,
                    trackerMotionDetected, fps.value());
            sendMotionNotifications();
        }

        private void updateMotionAndPresence() {
            // Motion history
            List<Rect> presenceRegions = motionDetector.motionContours
                    .regions();
            presenceHistory.add(presenceRegions);
            presenceRegion = motionHistory.region();
            // Remove potential blinking eyes from motion region history
            List<Rect> motionRegions = new Vector<Rect>();
            MatVector contours = motionDetector.motionContours.contours;
            for (int i = 0; i < motionRegions.size(); i++) {
                if (!isCircular(contours.get(i), 4)) {
                    motionRegions.add(presenceRegions.get(i));
                }
            }
            motionHistory.add(motionRegions);
            motionRegion = motionHistory.region();
            // Contour motion
            contourMotionDetected = motionRegions.size() > 0;
            // Tracker motion
            double distance2 = distanceTracker
                    .distance2(motionDetector.trackFeatures.keyPoints());
            trackerMotionDetected = distance2 > distanceThreshold2;
            // All together combined
            motionDetected = contourMotionDetected || trackerMotionDetected;
        }

        private void updateMotionArea() {
            final int motionArea;
            if (motionDetected) {
                motionArea = motionHistory.tail().area();
            } else {
                motionArea = 0;
            }
            synchronized (motionAreaHistory) {
                motionAreaHistory.add(motionArea);
            }
        }

        @Deprecated
        private void sendMotionNotifications() {
            // After setting current state, send notifications
            if (motionDetected) {
                // TODO signals MotionStart immediately without delay
                if (motionDetectedCounter < 0) {
                    // Motion just started
                    signalMotionStart();
                    printDebug();
                } else {
                    printDebug();
                }
                // TODO Check usage of motion history in basic motion
                // detector - inertia may add up this way,
                // and the javacv implementation will just rely on the
                // basic
                // implementation
                motionDetectedCounter = MotionInertia;
            } else {
                if (motionDetectedCounter == 0) {
                    printDebug();
                    signalMotionEnd();
                    motionDetectedCounter = -1;
                } else if (motionDetectedCounter > 0) {
                    // inertia
                    motionDetectedCounter--;
                }
            }
        }

        private void renderDebugInfo(final Mat videoImage, Rect r,
                boolean contourMotionDetected, boolean trackerMotionDetected,
                double fps) {
            if (r != MotionProcessorJavaCV.None) {
                if (debug) {
                    EnumSet<Presence> indicators = getPresence();
                    if (logDetails) {
                        logMotionState(indicators, contourMotionDetected,
                                trackerMotionDetected);
                    }
                    boolean present = indicators.contains(Presence.Present);
                    // Motion
                    if (indicators.contains(Presence.Shake)) {
                        Rect presenceRect = presenceIndicators
                                .get(Presence.Present);
                        rectangle(videoImage, presenceRect,
                                present ? MidBlue : DarkBlue, 15, 8, 0);
                    } else {
                        renderMotionRegion(videoImage, r, present);
                        renderPresenceIndicators(videoImage, r, indicators,
                                present);
                        motionDetector.trackFeatures.render(videoImage, Green);
                        // tracker distance
                        if (contourMotionDetected && !trackerMotionDetected) {
                            renderContourMotionRegion(videoImage, r);
                        } else if (trackerMotionDetected) {
                            renderDistanceTrackerPoints(videoImage);
                        }
                    }
                    renderFPS(fps, videoImage);
                    updateWindows(videoImage);
                }
            }
        }

        private void logMotionState(EnumSet<Presence> indicators,
                boolean contourMotionDetected, boolean trackerMotionDetected) {
            // Log state
            TeaseLib.instance().log.info(indicators.toString());
            TeaseLib.instance().log.info("contourMotionDetected="
                    + contourMotionDetected + "  trackerMotionDetected="
                    + trackerMotionDetected + "(distance=" + distanceTracker
                            .distance2(motionDetector.trackFeatures.keyPoints())
                    + ")");
        }

        private void renderMotionRegion(final Mat videoImage, Rect r,
                boolean present) {
            drawRect(videoImage, r, "", present ? Green : Blue);
            circle(videoImage, center(r), 2, present ? Green : Blue, 2, 8, 0);
        }

        private void renderContourMotionRegion(final Mat videoImage, Rect rM) {
            motionDetector.motionContours.render(videoImage, DarkRed, -1);
            putText(videoImage, rM.area() + "p2",
                    new Point(videoImage.cols() - 40, videoImage.cols() - 20),
                    FONT_HERSHEY_PLAIN, 2.75, White);
        }

        private void renderDistanceTrackerPoints(final Mat videoImage) {
            if (motionDetector.trackFeatures.haveFeatures()) {
                distanceTracker.renderDebug(videoImage,
                        motionDetector.trackFeatures.keyPoints());
            }
        }

        private void updateWindows(final Mat videoImage) {
            org.bytedeco.javacpp.opencv_highgui.imshow(INPUT, videoImage);
            org.bytedeco.javacpp.opencv_highgui.imshow(MOTION,
                    motionDetector.motion.output);
            if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
                // break;
            }
        }

        private void renderFPS(double fps, final Mat videoImage) {
            // fps
            String fpsFormatted = String.format("%1$.2f", fps);
            putText(videoImage, fpsFormatted + "fps", new Point(0, 40),
                    FONT_HERSHEY_PLAIN, 2.75, White);
        }

        private void renderPresenceIndicators(final Mat videoImage, Rect rM,
                EnumSet<Presence> indicators, boolean present) {
            // Presence indicators
            for (Map.Entry<Presence, Rect> entry : presenceIndicators
                    .entrySet()) {
                if (indicators.contains(entry.getKey())) {
                    if (entry.getKey() == Presence.Present) {
                        circle(videoImage, center(rM),
                                videoCaptureDevice.size().height() / 4,
                                present ? Green : Blue, 4, 4, 0);
                    } else {
                        rectangle(videoImage, entry.getValue(),
                                present ? Red : Blue, 4, 8, 0);
                    }
                }
            }
        }

        private void printDebug() {
            TeaseLib.instance().log.debug("MotionArea="
                    + ((DetectionEventsJavaCV) detectionEvents).motionRegion);
            TeaseLib.instance().log.debug("PresenceArea="
                    + ((DetectionEventsJavaCV) detectionEvents).presenceRegion);
        }
    }

    @Override
    protected int fps() {
        return desiredFPS;
    }

    @Override
    public void clearMotionHistory() {
        DetectionEventsJavaCV detectionEventsJavaCV = (DetectionEventsJavaCV) detectionEvents;
        detectionEventsJavaCV.motionHistory.clear();
        detectionEventsJavaCV.presenceHistory.clear();
    }
}
