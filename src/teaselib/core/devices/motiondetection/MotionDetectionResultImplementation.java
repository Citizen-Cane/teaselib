package teaselib.core.devices.motiondetection;

import static teaselib.core.javacv.util.Geom.intersects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.concurrency.Signal;
import teaselib.core.javacv.util.Geom;
import teaselib.core.util.TimeLine;
import teaselib.motiondetection.MotionDetector.Presence;

public class MotionDetectionResultImplementation extends MotionDetectionResultData {
    private static final Logger logger = LoggerFactory.getLogger(MotionDetectionResultImplementation.class);

    private static final double CircularityVariance = 1.3; // 1.3 seems to be necessary to detect blinking eyes
    private static final Set<Presence> STARTUP_PRESENCE = Collections.singleton(Presence.CameraShake);

    public class PresenceData {
        final Map<Presence, Rect> presenceIndicators;

        boolean trackerMotionDetected = false;
        boolean contourMotionDetected = false;;
        Set<Presence> indicators = STARTUP_PRESENCE;
        Set<Presence> debugIndicators = STARTUP_PRESENCE;
        Rect presenceRegion = null;
        Rect debugPresenceRegion = null;

        public PresenceData() {
            presenceIndicators = MotionDetectionResultImplementation.this.presenceIndicators;
        }

        public void update(double calculationTimeSpan, double debugWindowTimeSpan) {
            trackerMotionDetected = MotionDetectionResultImplementation.this.trackerMotionDetected;
            contourMotionDetected = MotionDetectionResultImplementation.this.contourMotionDetected;

            indicators = getIndicatorHistory(calculationTimeSpan);
            debugIndicators = getIndicatorHistory(debugWindowTimeSpan);
            presenceRegion = getPresenceRegion(calculationTimeSpan);
            debugPresenceRegion = getPresenceRegion(calculationTimeSpan);
        }
    }

    public PresenceData presenceData = new PresenceData();

    MotionDetectionResultImplementation(Size size) {
        super(size);
    }

    /**
     * @param videoImage
     * @param motionProcessor
     * @param timeStamp
     * @return True if the indicator state has changed
     */
    @Override
    public boolean updateMotionState(Mat videoImage, MotionProcessorJavaCV motionProcessor, long timeStamp) {
        updateMotionAndPresence(videoImage, motionProcessor, timeStamp);
        updateMotionTimeLine(timeStamp, motionProcessor);
        updateIndicatorTimeLine(timeStamp);
        // The data might not have changed, but because the inspected time intervals aren't fixed anymore,
        // the predicate used in await(...) might trigger because time has advanced
        return true;
    }

    private void updateMotionAndPresence(Mat videoImage, MotionProcessorJavaCV motionProcessor, long timeStamp) {
        // Motion history
        // TODO filter out camera shaking (actual shakes, light changes)
        // so that we don't have to use a fixed time span
        List<Rect> presenceRegions = motionProcessor.motionContours.regions();
        // moving forward in time changes the presence and motion regions,
        // so eventually it collapses to the latest presence region
        // TODO partition motion an presence regions by timespan and
        // include the group that intersects the requested timespan
        // -> then it's possible to join the whole motion region group
        // again, or the latest n members of the group.
        if (presenceRegions.isEmpty()) {
            // Even just adding the time stamp
            // makes our motion region fade away
            presenceRegionHistory.add(timeStamp);
        } else {
            presenceRegionHistory.add(Geom.join(presenceRegions), timeStamp);
        }
        // Remove blinking eyes from motion region history
        List<Rect> motionRegions = new ArrayList<>();
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
        // Contour motion
        contourMotionDetected = !motionRegions.isEmpty();
        // Tracker motion
        motionProcessor.distanceTracker.updateDistance(videoImage, contourMotionDetected);
        double distance2 = motionProcessor.distanceTracker.distance2();
        trackerMotionDetected = distance2 > motionProcessor.distanceThreshold2;
        // All together combined
        motionDetected = contourMotionDetected || trackerMotionDetected;
    }

    private void updateMotionTimeLine(long timeStamp, MotionProcessorJavaCV motionProcessor) {
        final int motionArea;
        if (contourMotionDetected) {
            motionArea = motionRegionHistory.tail().area();
        } else if (trackerMotionDetected) {
            motionArea = motionProcessor.distanceThreshold2 * motionProcessor.distanceThreshold2;
            motionProcessor.distanceTracker.restart();
        } else {
            motionArea = 0;
        }
        synchronized (motionAreaHistory) {
            motionAreaHistory.add(motionArea, timeStamp);
        }
    }

    private boolean updateIndicatorTimeLine(long timeStamp) {
        Set<Presence> indicators = getPresence(motionRegionHistory.tail(), presenceRegionHistory.tail());
        return indicatorHistory.add(indicators, timeStamp);
    }

    @Override
    public Rect getPresenceRegion(double seconds) {
        return Geom.join(presenceRegionHistory.getTimeSpan(seconds));
    }

    @Override
    public Rect getMotionRegion(double seconds) {
        return Geom.join(motionRegionHistory.getTimeSpan(seconds));
    }

    @Override
    public boolean await(Signal signal, final double amount, final Presence change, final double timeSpanSeconds,
            final double timeoutSeconds) throws InterruptedException {
        // TODO clamp amount to [0,1] and handle > 0.0, >= 1.0
        try {
            return signal.await(timeoutSeconds, (new Signal.HasChangedPredicate() {
                @Override
                public Boolean call() throws Exception {
                    List<TimeLine.Slice<Set<Presence>>> timeSpanIndicatorHistory = indicatorHistory
                            .getTimeSpanSlices(timeSpanSeconds);
                    return getAmount(timeSpanIndicatorHistory, change) >= amount;
                }
            }));
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    public static double getAmount(List<TimeLine.Slice<Set<Presence>>> slices, Object item) {
        if (slices.isEmpty()) {
            return 0.0;
        }
        long absoluteCoverage = 0;
        long absoluteTime = 0;
        for (Iterator<TimeLine.Slice<Set<Presence>>> it = slices.iterator(); it.hasNext();) {
            TimeLine.Slice<Set<Presence>> slice = it.next();
            final long increment;
            increment = slice.t;
            if (slice.item.contains(item)) {
                absoluteCoverage += increment;
            }
            absoluteTime += increment;
        }
        double amount = absoluteCoverage / (double) absoluteTime;
        return amount;
    }

    @Override
    @SuppressWarnings("resource")
    public Set<Presence> getPresence(Rect motionRegion, Rect presenceRegion) {
        Rect presenceRect = presenceIndicators.get(viewPoint2PresenceRegion.get(viewPoint));
        Point tl = presenceRect.tl();
        // rect.br() returns point + size which is not inside rect
        Point br = new Point(tl.x() + presenceRect.width() - 1, tl.y() + presenceRect.height() - 1);
        final Set<Presence> presence;
        try {
            if (motionRegion == null || (motionRegion.contains(tl) && motionRegion.contains(br))) {
                presence = shakePresence();
            } else {
                presence = presenceState(presenceRegion, presenceRect);
            }
        } finally {
            tl.close();
            br.close();
        }
        return presence;
    }

    private Set<Presence> shakePresence() {
        // Keep last state, to avoid signaling changes during shakes
        Set<Presence> last = new LinkedHashSet<>(indicatorHistory.tail());
        last.remove(Presence.NoCameraShake);
        last.add(Presence.CameraShake);
        return last;
    }

    private Set<Presence> presenceState(Rect presenceRegion, Rect presenceRect) {
        boolean presenceInsidePresenceRect = intersects(presenceRegion, presenceRect);
        boolean motionDetected = motionAreaHistory.tail() > 0.0;
        Presence presenceState = motionDetected || presenceInsidePresenceRect ? Presence.Present : Presence.Away;
        Set<Presence> directions = new LinkedHashSet<>();
        directions.add(presenceState);
        if (motionDetected) {
            directions.add(Presence.Motion);
        } else {
            directions.add(Presence.NoMotion);
        }
        // Presence regions
        for (Map.Entry<Presence, Rect> e : presenceIndicators.entrySet()) {
            Presence key = e.getKey();
            if (key != Presence.Present) {
                if (intersects(e.getValue(), presenceRegion)) {
                    // Intersects region
                    directions.add(key);
                } else if (negatedRegions.containsKey(key)) {
                    // Doesn't intersect region
                    directions.add(negatedRegions.get(key));
                }
            }
        }
        directions.add(Presence.NoCameraShake);
        return directions;
    }

    public void updateRenderData(double calculationTimeSpan, double debugWindowTimeSpan) {
        presenceData.update(calculationTimeSpan, debugWindowTimeSpan);
    }
}
