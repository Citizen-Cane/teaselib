/**
 * 
 */
package teaselib.core.devices.motiondetection;

import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static teaselib.core.javacv.Color.Blue;
import static teaselib.core.javacv.Color.DarkBlue;
import static teaselib.core.javacv.Color.DarkGreen;
import static teaselib.core.javacv.Color.Green;
import static teaselib.core.javacv.Color.White;
import static teaselib.core.javacv.util.Gui.drawRect;

import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.javacv.Color;
import teaselib.core.javacv.HeadGestureTracker;
import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector.Presence;

/**
 * OpenCV imshow(...) isn't thread safe, it may hang (at least on windows) if called from several threads.
 */
public class MotionDetectorJavaCVDebugRenderer {
    private final Size windowSize;
    final Thread owner;

    public MotionDetectorJavaCVDebugRenderer(Size windowSize) {
        this.windowSize = windowSize;
        this.owner = Thread.currentThread();
    }

    public void render(Mat debugOutput, MotionProcessorJavaCV.MotionData pixelData,
            MotionDetectionResultImplementation.PresenceData resultData, HeadGestureTracker gestureTracker,
            Gesture gesture, double fps) {
        boolean present = resultData.debugIndicators.contains(Presence.Present);
        // Motion
        if (resultData.debugIndicators.contains(Presence.CameraShake)) {
            rectangle(debugOutput, resultData.presenceIndicators.get(Presence.Present),
                    present ? Color.MidBlue : Color.DarkBlue, 15, 8, 0);
        } else {
            if (resultData.debugPresenceRegion != null) {
                renderMotionRegion(debugOutput, resultData.debugPresenceRegion, present);
            }
            renderPresenceIndicators(debugOutput, resultData.debugPresenceRegion, resultData.presenceIndicators,
                    resultData.debugIndicators, present);

            // TODO Enable from detector
            // if (renderData.contourMotionDetected) {
            // renderContourMotionRegion(debugOutput, renderData.debugPresenceRegion);
            // }

            // TODO Enable from detector
            // if (resultData.trackerMotionDetected) {
            // renderDistanceTrackerPoints(debugOutput, pixelData);
            // }

            if (gestureTracker.hasFeatures()) {
                gestureTracker.render(debugOutput);
            }

            renderGesture(debugOutput, gestureTracker, gesture);
        }
        renderRegionList(debugOutput, resultData.debugIndicators);
        renderFPS(debugOutput, fps);
    }

    private static void renderMotionRegion(Mat debugOutput, Rect r, boolean present) {
        drawRect(debugOutput, r, "", present ? Green : Blue);
    }

    // private void renderContourMotionRegion(Mat debugOutput, Rect rM) {
    // motionProcessor.motionContours.render(debugOutput, -1, White);
    // if (rM != null) {
    // Point p = new Point(debugOutput.cols() - 40, debugOutput.cols() - 20);
    // putText(debugOutput, rM.area() + "p2", p, FONT_HERSHEY_PLAIN, 2.75, White);
    // p.close();
    // }
    // }

    // private void renderDistanceTrackerPoints(Mat debugOutput, MotionProcessorJavaCV.MotionData motionData) {
    //     MotionProcessorJavaCV.render(debugOutput, motionData, motionData.color);
    // }

    private void renderGesture(Mat debugOutput, HeadGestureTracker gestureTracker, Gesture gesture) {
        rectangle(debugOutput, gestureTracker.getRegion(), gesture == Gesture.None ? Color.MidCyan : Color.Cyan, 4, 8,
                0);

        if (gesture != Gesture.None) {
            Point p = new Point(debugOutput.size().width() - 70, 30);
            putText(debugOutput, gesture.toString(), p, FONT_HERSHEY_PLAIN, 2.5, Color.Red /* gestureTracker.color */);
            p.close();
        }
    }

    private static void renderRegionList(Mat debugOutput, Set<Presence> indicators) {
        int n = 0;
        int s = 14;
        for (Presence indicator : indicators) {
            Point p = new Point(0, 20 + n);
            putText(debugOutput, indicator.toString(), p, FONT_HERSHEY_PLAIN, 1.25, White);
            n += s;
            p.close();
        }
    }

    private static void renderFPS(Mat debugOutput, double fps) {
        // fps
        String fpsFormatted = String.format("%1$.2f", fps);
        Point p = new Point(0, debugOutput.rows() - 10);
        putText(debugOutput, fpsFormatted + "fps", p, FONT_HERSHEY_PLAIN, 1.75, White);
        p.close();
    }

    private void renderPresenceIndicators(Mat debugOutput, Rect rM, Map<Presence, Rect> presenceIndicators,
            Set<Presence> indicators, boolean present) {
        for (Presence key : Presence.values()) {
            if (indicators.contains(key) && presenceIndicators.containsKey(key)) {
                rectangle(debugOutput, presenceIndicators.get(key), present ? DarkGreen : DarkBlue, 4, 8, 0);
            }
        }
    }

    public void close() {
        windowSize.close();
    }
}
