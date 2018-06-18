package teaselib.core.devices.motiondetection;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.util.TimeLine;
import teaselib.motiondetection.MotionDetector;
import teaselib.motiondetection.MotionDetector.MotionSensitivity;
import teaselib.motiondetection.MotionDetector.Presence;
import teaselib.motiondetection.ViewPoint;

/**
 * @author Citizen-Cane
 *
 */
public class MotionSource extends PerceptionSource<Presence> {
    @FunctionalInterface
    public interface MotionFunction {
        boolean matches(MotionDetectionResult result);
    }

    final MotionProcessorJavaCV motionProcessor;
    MotionDetectionResultImplementation presenceResult;

    private static final Map<MotionSensitivity, Integer> motionSensitivities = initStructuringElementSizes();

    private static EnumMap<MotionSensitivity, Integer> initStructuringElementSizes() {
        EnumMap<MotionSensitivity, Integer> map = new EnumMap<>(MotionSensitivity.class);
        map.put(MotionSensitivity.High, 12);
        map.put(MotionSensitivity.Normal, 24);
        map.put(MotionSensitivity.Low, 36);
        return map;
    }

    public MotionSource() {
        motionProcessor = new MotionProcessorJavaCV();
    }

    @Override
    public void setResulution(Size size) {
        super.setResulution(size);
        presenceResult = new MotionDetectionResultImplementation(size);
    }

    public void applySensitivity(Size capture, Size processing, MotionSensitivity motionSensitivity) {
        motionProcessor.setStructuringElementSize(MotionProcessorJavaCV.sizeOfStructuringElement(capture, processing,
                motionSensitivities.get(motionSensitivity)));
    }

    public void applyPointOfView(ViewPoint viewPoint) {
        presenceResult.setViewPoint(viewPoint);
    }

    @Override
    public void update(Mat video, long timeStamp) {
        motionProcessor.update(video);
        motionProcessor.updateTrackerData(video);

        signal.doLocked(() -> {
            boolean hasChanged = presenceResult.updateMotionState(video, motionProcessor, timeStamp);
            if (hasChanged) {
                signal.signal();
            }
        });
    }

    public boolean await(double amount, Presence change, double timeSpanSeconds, final double timeoutSeconds) {
        // TODO clamp amount to [0,1] and handle > 0.0, >= 1.0
        return await(() -> getChange(amount, change, timeSpanSeconds), timeoutSeconds);
    }

    private Boolean getChange(double amount, Presence change, double timeSpanSeconds) {
        List<TimeLine.Slice<Set<Presence>>> timeSpanIndicatorHistory = presenceResult.indicatorHistory
                .getTimeSpanSlices(timeSpanSeconds);
        return MotionDetectionResultImplementation.getAmount(timeSpanIndicatorHistory, change) >= amount;
    }

    public boolean await(MotionFunction expected, double timeoutSeconds) {
        return super.await(() -> expected.matches(presenceResult), timeoutSeconds);
    }

    @Override
    void startNewRecognition() {
        // Ignore since motion is continuous, not an event like a gesture
    }

    public void updateResult(Rect region) {
        presenceResult.presenceData.contourMotionDetected = true;
        presenceResult.presenceData.trackerMotionDetected = true;
        presenceResult.presenceData.indicators = Collections.singleton(Presence.Center);
        presenceResult.presenceData.debugIndicators = Collections.singleton(Presence.Center);
        presenceResult.presenceData.presenceRegion = region;
    }

    public void updatePresenceResult() {
        motionProcessor.updateRenderData();
        // TODO parameter of capture thread or debug renderer
        double debugWindowTimeSpan = 1.0;
        presenceResult.updateRenderData(MotionDetector.PresenceRegionDefaultTimespan, debugWindowTimeSpan);
    }

    public Rect defaultRegion() {
        return presenceResult.presenceData.presenceIndicators.get(Presence.Center);
    }

}
