/**
 * 
 */
package teaselib.core.devices.motiondetection;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static teaselib.core.javacv.Color.*;
import static teaselib.core.javacv.util.Gui.*;

import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.javacv.Color;
import teaselib.core.javacv.Contours;
import teaselib.core.javacv.HeadGestureTracker;
import teaselib.motiondetection.Gesture;
import teaselib.motiondetection.MotionDetector.Presence;

public class MotionDetectorJavaCVDebugRenderer {
    private final Size windowSize;

    public MotionDetectorJavaCVDebugRenderer(Size windowSize) {
        this.windowSize = windowSize;
    }

    public void render(Mat debugOutput, Contours contours, MotionProcessorJavaCV.MotionData pixelData,
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
            renderPresenceIndicators(debugOutput, resultData.presenceIndicators, resultData.debugIndicators, present);

            // TODO Enable from detector when logging details
            if (resultData.contourMotionDetected) {
                renderContourMotionRegion(contours, debugOutput, resultData.debugPresenceRegion);
            }

            // TODO Enable from detector when logging details
            if (resultData.trackerMotionDetected) {
                renderDistanceTrackerPoints(debugOutput, pixelData);
            }

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

    private static void renderContourMotionRegion(Contours contours, Mat debugOutput, Rect rM) {
        contours.render(debugOutput, -1, White);
        if (rM != null) {
            try (Point p = new Point(debugOutput.cols() - 40, debugOutput.cols() - 20);) {
                putText(debugOutput, rM.area() + "p2", p, FONT_HERSHEY_PLAIN, 2.75, White);
            }
        }
    }

    private static void renderDistanceTrackerPoints(Mat debugOutput, MotionProcessorJavaCV.MotionData motionData) {
        MotionProcessorJavaCV.render(debugOutput, motionData, motionData.color);
    }

    private static void renderGesture(Mat debugOutput, HeadGestureTracker gestureTracker, Gesture gesture) {
        if (gestureTracker.getRegion() != null) {
            rectangle(debugOutput, gestureTracker.getRegion(), gesture == Gesture.None ? Color.MidCyan : Color.Cyan, 4,
                    8, 0);
        }

        if (gesture != Gesture.None) {
            try (Point p = new Point(debugOutput.size().width() - 70, 30);) {
                putText(debugOutput, gesture.toString(), p, FONT_HERSHEY_PLAIN, 2.5,
                        Color.Red /* gestureTracker.color */);
            }
        }
    }

    private static void renderRegionList(Mat debugOutput, Set<Presence> indicators) {
        int n = 0;
        int s = 14;
        for (Presence indicator : indicators) {
            try (Point p = new Point(0, 20 + n);) {
                putText(debugOutput, indicator.toString(), p, FONT_HERSHEY_PLAIN, 1.25, White);
                n += s;
            }
        }
    }

    private static void renderFPS(Mat debugOutput, double fps) {
        // fps
        String fpsFormatted = String.format("%1$.2f", fps);
        try (Point p = new Point(0, debugOutput.rows() - 10);) {
            putText(debugOutput, fpsFormatted + "fps", p, FONT_HERSHEY_PLAIN, 1.75, White);
        }
    }

    private static void renderPresenceIndicators(Mat debugOutput, Map<Presence, Rect> presenceIndicators,
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
