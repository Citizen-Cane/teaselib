package teaselib.motiondetection.javacv;

import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static teaselib.motiondetection.javacv.util.Geom.center;
import static teaselib.motiondetection.javacv.util.Geom.intersects;
import static teaselib.motiondetection.javacv.util.Gui.drawRect;
import static teaselib.motiondetection.javacv.util.Gui.positionWindows;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.TeaseLib;
import teaselib.motiondetection.BasicMotionDetector;
import teaselib.motiondetection.DeviceCache;
import teaselib.motiondetection.javacv.util.FramesPerSecond;

public class MotionDetectorJavaCV extends BasicMotionDetector {
    public static final String DeviceClassName = "MotionDetectorJavaCV";

    public static final EnumSet<Feature> Features = EnumSet.of(Feature.Motion,
            Feature.Presence);

    private static final Scalar Red = new Scalar(0, 0, 255, 0);
    private static final Scalar Green = new Scalar(0, 255, 0, 0);
    private static final Scalar Blue = new Scalar(255, 0, 0, 0);
    private static final Scalar White = new Scalar(255, 255, 255, 0);
    private static final Scalar MidBlue = new Scalar(128, 0, 0, 0);
    private static final Scalar DarkRed = new Scalar(0, 0, 64, 0);
    private static final Scalar DarkGreen = new Scalar(0, 64, 0, 0);
    private static final Scalar DarkBlue = new Scalar(64, 0, 0, 0);

    private static final Map<MotionSensitivity, Integer> motionSensitivities = new HashMap<>(
            initStructuringElementSizes());

    private static Map<MotionSensitivity, Integer> initStructuringElementSizes() {
        Map<MotionSensitivity, Integer> map = new HashMap<>();
        map.put(MotionSensitivity.High, 6);
        map.put(MotionSensitivity.Normal, 18);
        map.put(MotionSensitivity.Low, 36);
        return map;
    }

    private final VideoCaptureDevice videoCaptureDevice;

    public MotionDetectorJavaCV(VideoCaptureDevice videoCaptureDevice) {
        // TODO Remove id altogether
        super(DeviceCache.getDeviceName("test"));
        this.videoCaptureDevice = videoCaptureDevice;
        detectionEvents = new DetectionEventsJavaCV();
        setSensitivity(MotionSensitivity.Normal);
        detectionEvents.start();
    }

    @Override
    public void setSensitivity(MotionSensitivity motionSensivity) {
        pause();
        ((DetectionEventsJavaCV) detectionEvents).motionDetector
                .setStructuringElementSize(
                        motionSensitivities.get(motionSensivity));
        resume();
    }

    @Override
    public EnumSet<Feature> getFeatures() {
        return Features;
    }

    @Override
    protected boolean isMotionDetected(int pastFrames) {
        synchronized (mi) {
            double dm = mi.getMotion(pastFrames);
            return dm > 0.0;
        }
    }

    @Override
    public EnumSet<Presence> getPresence() {
        Rect r = ((DetectionEventsJavaCV) detectionEvents).motionRegion;
        return getPresence(r);
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

        private final MotionProcessor motionDetector;
        private final Map<Presence, Rect> presenceIndicators;
        private int motionDetectedCounter = -1;

        private Rect motionRegion;

        boolean debug = true;

        DetectionEventsJavaCV() {
            super();
            setDaemon(true);
            videoCaptureDevice.open(new Size(320, 240));
            Size size = videoCaptureDevice.size();
            motionDetector = new MotionProcessor(
                    videoCaptureDevice.captureSize().width(), size.width());
            presenceIndicators = buildPresenceIndicatorMap(size);
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
            int minimalFps = 15;
            final int desiredFPS = getDesiredFPS(videoCaptureDevice,
                    minimalFps);
            long desiredFrameTime = 1000 / desiredFPS;
            FramesPerSecond fps = new FramesPerSecond(desiredFPS);
            debug = true;
            motionRegion = MotionProcessor.None;
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
                detectionEvents.lockStartStop.lock();
                motionDetector.update(videoImage);
                lockStartStop.unlock();
                // Resulting bounding boxes
                motionRegion = motionDetector.region();
                updateMotionState(motionRegion);
                renderDebugInfo(fps.value(), videoImage, motionRegion);
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
            cleanupResources();
        }

        private void updateMotionState(Rect rM) {
            boolean motionDetected = motionDetector.motionContours.contours
                    .size() > 0;
            // Build motion and direction frames history
            final int motionArea;
            if (motionDetected) {
                motionArea = rM.area();
            } else {
                motionArea = 0;
            }
            synchronized (mi) {
                mi.add(motionArea);
            }
            // After setting current state, send notifications
            if (motionDetected) {
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

        private void renderDebugInfo(double fps, final Mat videoImage,
                Rect rM) {
            if (rM != MotionProcessor.None) {
                if (debug) {
                    Rect r = motionDetector.motionRect();
                    EnumSet<Presence> indicators = getPresence(r);
                    boolean present = indicators.contains(Presence.Present);
                    // present = motionDetector.motionContours.contours.size() >
                    // 1;
                    // Motion
                    if (indicators.contains(Presence.Shake)) {
                        Rect presenceRect = presenceIndicators
                                .get(Presence.Present);
                        rectangle(videoImage, presenceRect,
                                present ? MidBlue : DarkBlue, 15, 8, 0);
                        putText(videoImage, "?", center(presenceRect),
                                FONT_HERSHEY_PLAIN, 10, White);
                    } else {
                        motionDetector.motionContours.render(videoImage,
                                DarkRed, -1);
                        drawRect(videoImage, rM,
                                Integer.toString(
                                        motionDetector.motionContours.pixels())
                                + ": " + rM.area() + "p2",
                                present ? Green : Blue);
                        circle(videoImage, center(rM), 15,
                                present ? Green : Blue, 12, 8, 0);
                        // Presence indicators
                        for (Map.Entry<Presence, Rect> entry : presenceIndicators
                                .entrySet()) {
                            if (indicators.contains(entry.getKey())) {
                                if (entry.getKey() == Presence.Present) {
                                    circle(videoImage, center(rM), 8,
                                            present ? Green : Blue,
                                            videoCaptureDevice.size().height()
                                                    / 2,
                                            4, 0);
                                } else {
                                    rectangle(videoImage, entry.getValue(),
                                            present ? Red : Blue, 4, 8, 0);
                                }
                            }
                        }
                    }
                    // fps
                    String fpsFormatted = String.format("%1$.2f", fps);
                    putText(videoImage, fpsFormatted + "fps", new Point(0, 40),
                            FONT_HERSHEY_PLAIN, 2.75, White);
                    org.bytedeco.javacpp.opencv_highgui.imshow(INPUT,
                            videoImage);
                    org.bytedeco.javacpp.opencv_highgui.imshow(MOTION,
                            motionDetector.motion.output);
                    if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
                        // break;
                    }
                }
            }
        }

        // TODO fps is double
        private int getDesiredFPS(VideoCaptureDevice videoCaptureDevice,
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

        private void cleanupResources() {
        }

        private void printDebug() {
            TeaseLib.instance().log.debug("MotionArea="
                    + ((DetectionEventsJavaCV) detectionEvents).motionRegion);
        }
    }
}
