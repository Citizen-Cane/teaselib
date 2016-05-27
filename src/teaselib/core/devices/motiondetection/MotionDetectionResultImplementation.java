/**
 * 
 */
package teaselib.core.devices.motiondetection;

import static teaselib.core.javacv.util.Geom.*;

import java.util.LinkedHashSet;
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
import teaselib.motiondetection.MotionDetector.Presence;

public class MotionDetectionResultImplementation
        extends MotionDetectionResultData {

    // TODO exactly define Circularity and its calculation
    private static final double CircularityVariance = 1.3; // 1.3 seems to
                                                           // be necessary
                                                           // to detect
                                                           // blinking eye
                                                           // balls

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
    public boolean updateMotionState(Mat videoImage,
            MotionProcessorJavaCV motionProcessor, long timeStamp) {
        updateMotionAndPresence(videoImage, motionProcessor, timeStamp);
        updateMotionTimeLine(timeStamp, motionProcessor);
        updateIndicatorTimeLine(timeStamp);
        // The data might not have changed,
        // but because the inspected time intervals aren't fixed anymore,
        // the predicate used in await(...) might trigger because the time has
        // advanced
        return true;
    }

    private void updateMotionAndPresence(Mat videoImage,
            MotionProcessorJavaCV motionProcessor, long timeStamp) {
        // Motion history
        // TODO filter out Shakes (actual shakes, light changes)
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
        Set<Presence> indicators = getPresence(motionRegionHistory.tail(),
                presenceRegionHistory.tail());
        boolean hasChanged = indicatorHistory.add(indicators, timeStamp);
        return hasChanged;
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
    public boolean awaitChange(Signal signal, final double amount,
            final Presence change, final double timeSpanSeconds,
            final double timeoutSeconds) {
        // TODO clamp amount to [0,1] and handle > 0.0, >= 1.0
        try {
            return signal.awaitChange(timeoutSeconds,
                    (new Signal.HasChangedPredicate() {
                        @Override
                        public Boolean call() throws Exception {
                            List<Set<Presence>> timeSpanIndicatorHistory = indicatorHistory
                                    .getTimeSpan(timeSpanSeconds);
                            // No need to join any sets, since we can just
                            // calculate the occurrence percentage
                            int n = numberOfOccurences(change,
                                    timeSpanIndicatorHistory);
                            int s = timeSpanIndicatorHistory.size();
                            return ((double) n) / s >= amount;
                        }
                    }));
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } catch (Exception e) {
            TeaseLib.instance().log.error(this, e);
        }
        return false;
    }

    // TODO write test
    int numberOfOccurences(Object item, List<Set<Presence>> collection) {
        int n = 0;
        for (Set<? extends Object> set : collection) {
            if (set.contains(item))
                n++;
        }
        return n;
    }

    @SuppressWarnings("resource")
    @Override
    public Set<Presence> getPresence(Rect motionRegion, Rect presenceRegion) {
        Rect presenceRect = presenceIndicators.get(Presence.Present);
        if (motionRegion == null || (motionRegion.contains(presenceRect.tl())
                && motionRegion.contains(presenceRect.br()))) {
            // Keep last state, to avoid signaling changes during shakes
            Set<Presence> last = new LinkedHashSet<Presence>(
                    indicatorHistory.tail());
            last.remove(Presence.NoShake);
            last.add(Presence.Shake);
            return last;
        } else {
            boolean presenceInsidePresenceRect = intersects(presenceRegion,
                    presenceRect);
            boolean motionDetected = motionAreaHistory.tail() > 0.0;
            Presence presenceState = motionDetected
                    || presenceInsidePresenceRect ? Presence.Present
                            : Presence.Away;
            Set<Presence> directions = new LinkedHashSet<Presence>();
            directions.add(Presence.NoShake);
            directions.add(presenceState);
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
            if (motionDetected) {
                directions.add(Presence.Motion);
            } else {
                directions.add(Presence.NoMotion);
            }
            return directions;
        }
    }
}
