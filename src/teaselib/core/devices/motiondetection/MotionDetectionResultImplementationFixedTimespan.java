/**
 * 
 */
package teaselib.core.devices.motiondetection;

import static teaselib.core.javacv.util.Geom.*;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.TeaseLib;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.concurrency.Signal;
import teaselib.core.javacv.util.Geom;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.util.math.Statistics;

public class MotionDetectionResultImplementationFixedTimespan
        extends MotionDetectionResultData {
    // TODO exactly define Circularity and its calculation
    private static final double CircularityVariance = 1.3; // 1.3 seems to
                                                           // be necessary
                                                           // to detect
                                                           // blinking eye
                                                           // balls
    protected Rect motionRegion = null;
    protected Rect presenceRegion = null;

    MotionDetectionResultImplementationFixedTimespan(Size size) {
        super(size);
    }

    /**
     * @param videoImage
     * @param motionProcessor
     * @param timeStamp
     * @return True if the indicator state has changed
     */
    @Override
    public boolean updateMotionState(Mat videoImage,
            MotionProcessorJavaCV motionProcessor, long timeStamp) {
        updateMotionAndPresence(videoImage, motionProcessor, timeStamp);
        updateMotionTimeLine(timeStamp, motionProcessor);
        return updateIndicatorTimeLine(timeStamp);
    }

    private void updateMotionAndPresence(Mat videoImage,
            MotionProcessorJavaCV motionProcessor, long timeStamp) {
        // Motion history
        // TODO filter out Shakes (actual shakes, light changes)
        // TODO cluster motion an presence regions by time
        // so that we don't have to use a fixed time span
        List<Rect> presenceRegions = motionProcessor.motionContours.regions();
        if (presenceRegions.isEmpty()) {
            // Even just adding the time stamp
            // makes our motion region fade away
            presenceRegionHistory.add(timeStamp);
        } else {
            presenceRegionHistory.add(Geom.join(presenceRegions), timeStamp);
        }
        // moving forward in time changes the presence region, so
        // eventually it collapses on the current presence region
        // however we just want presence, no more,
        // and blinking eyes in the border regions
        // should count as "Away" anyway
        // -> no memory for presence needed
        presenceRegion = Geom.join(presenceRegionHistory
                .getTimeSpan(MotionDetector.PresenceRegionDefaultTimespan));
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
        motionRegion = Geom.join(motionRegionHistory
                .getTimeSpan(MotionDetector.MotionRegionDefaultTimespan));
        // Contour motion
        contourMotionDetected = motionRegions.size() > 0;
        // Tracker motion
        motionProcessor.distanceTracker.update(videoImage,
                contourMotionDetected, motionProcessor.trackFeatures);
        double distance2 = motionProcessor.distanceTracker
                .distance2(motionProcessor.trackFeatures.keyPoints());
        trackerMotionDetected = distance2 > motionProcessor.distanceThreshold2;
        // All together combined
        motionDetected = contourMotionDetected || trackerMotionDetected;
    }

    private void updateMotionTimeLine(long timeStamp,
            MotionProcessorJavaCV motionProcessor) {
        final int motionArea;
        if (contourMotionDetected) {
            motionArea = motionRegionHistory.tail().area();
        } else if (trackerMotionDetected) {
            motionArea = motionProcessor.distanceThreshold2
                    * motionProcessor.distanceThreshold2;
            motionProcessor.distanceTracker.reset();
        } else {
            motionArea = 0;
        }
        synchronized (motionAreaHistory) {
            motionAreaHistory.add(motionArea, timeStamp);
        }
    }

    private boolean updateIndicatorTimeLine(long timeStamp) {
        Set<Presence> indicators = getPresence(
                MotionDetector.PresenceRegionDefaultTimespan);
        boolean hasChanged = indicatorHistory.add(indicators, timeStamp);
        return hasChanged;
    }

    @Override
    public Rect getPresenceRegion(double seconds) {
        return presenceRegion;
    }

    @Override
    public Rect getMotionRegion(double seconds) {
        return motionRegion;
    }

    @Override
    public boolean awaitChange(Signal signal, double amount,
            final Presence change, double timeSpanSeconds,
            double timeoutSeconds) {
        try {
            return signal.awaitChange(timeoutSeconds,
                    (new Signal.HasChangedPredicate() {
                        @Override
                        public Boolean call() throws Exception {
                            Set<Presence> current = getPresence(
                                    MotionDetector.PresenceRegionDefaultTimespan);
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

    private Set<Presence> getPresence(double seconds) {
        return getPresence(getMotionRegion(seconds),
                getPresenceRegion(seconds));
    }

    @SuppressWarnings("resource")
    @Override
    public Set<Presence> getPresence(Rect motionRegion, Rect presenceRegion) {
        boolean motionDetected = isMotionDetected(
                MotionDetector.MotionRegionDefaultTimespan);
        Rect presenceRect = presenceIndicators.get(Presence.Present);
        if (motionRegion == null || (motionRegion.contains(presenceRect.tl())
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
            Set<Presence> directions = new HashSet<Presence>();
            for (Map.Entry<Presence, Rect> e : presenceIndicators.entrySet()) {
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

    private boolean isMotionDetected(final double seconds) {
        Statistics statistics = new Statistics(
                motionAreaHistory.getTimeSpan(seconds));
        double motion = statistics.max();
        return motion > 0.0;
    }
}
