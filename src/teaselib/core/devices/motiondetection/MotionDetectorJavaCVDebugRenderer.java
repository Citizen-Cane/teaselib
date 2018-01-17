/**
 * 
 */
package teaselib.core.devices.motiondetection;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static teaselib.core.javacv.Color.*;
import static teaselib.core.javacv.util.Geom.*;
import static teaselib.core.javacv.util.Gui.*;

import java.util.ConcurrentModificationException;
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
    private final MotionProcessorJavaCV motionProcessor;
    private final Size windowSize;

    final Thread owner;

    public MotionDetectorJavaCVDebugRenderer(MotionProcessorJavaCV motionProcessor, Size windowSize) {
        this.motionProcessor = motionProcessor;
        this.windowSize = windowSize;
        this.owner = Thread.currentThread();
    }

    public void render(Mat debugOutput, Rect r, Map<Presence, Rect> presenceIndicators, Set<Presence> indicators,
            boolean contourMotionDetected, boolean trackerMotionDetected, HeadGestureTracker gestureTracker,
            Gesture gesture, double fps) {
        if (Thread.currentThread() != owner) {
            throw new ConcurrentModificationException(
                    "Rendering to Open CV window works only in the thread that created the window: " + owner.toString()
                            + "!=" + Thread.currentThread().toString());
        }
        boolean present = indicators.contains(Presence.Present);
        // Motion
        if (indicators.contains(Presence.CameraShake)) {
            rectangle(debugOutput, presenceIndicators.get(Presence.Present), present ? Color.MidBlue : Color.DarkBlue,
                    15, 8, 0);
        } else {
            if (r != null) {
                renderMotionRegion(debugOutput, r, present);
            }
            renderPresenceIndicators(debugOutput, r, presenceIndicators, indicators, present);

            if (contourMotionDetected) {
                renderContourMotionRegion(debugOutput, r);
            }
            if (trackerMotionDetected) {
                renderDistanceTrackerPoints(debugOutput);
            }

            if (gestureTracker.hasFeatures()) {
                gestureTracker.render(debugOutput);
            }

            if (gesture != Gesture.None) {
                renderGesture(debugOutput, gestureTracker, gesture);
            }
        }
        renderRegionList(debugOutput, indicators);
        renderFPS(debugOutput, fps);
    }

    private static void renderMotionRegion(Mat debugOutput, Rect r, boolean present) {
        drawRect(debugOutput, r, "", present ? Green : Blue);
        circle(debugOutput, center(r), 2, present ? Green : Blue, 2, 8, 0);
    }

    private void renderContourMotionRegion(Mat debugOutput, Rect rM) {
        motionProcessor.motionContours.render(debugOutput, -1, White);
        if (rM != null) {
            Point p = new Point(debugOutput.cols() - 40, debugOutput.cols() - 20);
            putText(debugOutput, rM.area() + "p2", p, FONT_HERSHEY_PLAIN, 2.75, White);
            p.close();
        }
    }

    private void renderDistanceTrackerPoints(Mat debugOutput) {
        if (motionProcessor.distanceTracker.hasFeatures()) {
            motionProcessor.distanceTracker.render(debugOutput);
        }
    }

    private void renderGesture(Mat debugOutput, HeadGestureTracker gestureTracker, Gesture gesture) {
        Point p = new Point(debugOutput.size().width() - 70, 30);
        putText(debugOutput, gesture.toString(), p, FONT_HERSHEY_PLAIN, 2.5, Color.Red /* gestureTracker.color */);
        p.close();
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
                if (key == Presence.Present) {
                    circle(debugOutput, center(rM), windowSize.height() / 4, present ? MidGreen : MidBlue, 4, 4, 0);
                } else {
                    rectangle(debugOutput, presenceIndicators.get(key), present ? DarkGreen : DarkBlue, 4, 8, 0);
                }
            }
        }
    }

    public void close() {
        windowSize.close();
    }
}
