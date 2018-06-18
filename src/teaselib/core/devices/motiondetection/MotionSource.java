package teaselib.core.devices.motiondetection;

import org.bytedeco.javacpp.opencv_core.Mat;

import teaselib.motiondetection.MotionDetector.Presence;

/**
 * @author Citizen-Cane
 *
 */
public class MotionSource extends PerceptionSource<Presence> {

    @Override
    public void update(Mat video, long timeStamp) {
        // TODO Auto-generated method stub

    }

    @Override
    boolean active() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    void startNewRecognition() {
        // TODO Auto-generated method stub

    }

    @Override
    void resetCurrent() {
        // TODO Auto-generated method stub

    }

}
