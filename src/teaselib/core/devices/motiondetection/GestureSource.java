package teaselib.core.devices.motiondetection;

import java.util.function.BooleanSupplier;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;

import teaselib.core.javacv.Color;
import teaselib.core.javacv.HeadGestureTracker;
import teaselib.motiondetection.Gesture;

final class GestureSource extends PerceptionSource<Gesture> {
    final HeadGestureTracker gestureTracker = new HeadGestureTracker(Color.Cyan);
    final HeadGestureTracker.Parameters gestureResult = new HeadGestureTracker.Parameters();
    final BooleanSupplier activeState;

    public GestureSource(BooleanSupplier activeState) {
        super();
        this.activeState = activeState;
    }

    @Override
    boolean active() {
        return activeState.getAsBoolean();
    }

    @Override
    public void update(Mat video, long timeStamp) {
        if (gestureResult.cameraShake) {
            gestureTracker.clear();
            gestureTracker.findNewFeatures(video, defaultRegion(video));
        } else {
            gestureTracker.update(video, gestureResult.motionDetected, gestureResult.gestureRegion, timeStamp);
        }
        Gesture newGesture = gestureTracker.getGesture();
        boolean changed = current.get() != newGesture;
        current.set(newGesture);
        if (changed) {
            signal.signal();
        }
    }

    private static Rect defaultRegion(Mat video) {
        return new Rect(video.cols() / 4, video.rows() / 4, video.cols() / 2, video.rows() / 2);
    }

    public void updateResult(boolean cameraShake, boolean motionDetected, Rect region) {
        gestureResult.cameraShake = cameraShake;
        gestureResult.motionDetected = motionDetected;
        gestureResult.gestureRegion = region;

    }

    @Override
    void startNewRecognition() {
        gestureTracker.clear();
    }

    @Override
    void resetCurrent() {
        current.set(Gesture.None);
        // TODO integrate into clear()
    }
}