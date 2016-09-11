/**
 * 
 */
package teaselib.core.devices.motiondetection;

import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static teaselib.core.javacv.Color.Blue;
import static teaselib.core.javacv.Color.DarkBlue;
import static teaselib.core.javacv.Color.DarkGreen;
import static teaselib.core.javacv.Color.Green;
import static teaselib.core.javacv.Color.MidBlue;
import static teaselib.core.javacv.Color.MidGreen;
import static teaselib.core.javacv.Color.White;
import static teaselib.core.javacv.util.Geom.center;
import static teaselib.core.javacv.util.Gui.drawRect;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.core.javacv.Color;
import teaselib.motiondetection.MotionDetector.Presence;

/**
 * OpenCV imshow(...) isn't thread safe, it may hang (at least on windows) if
 * called from several threads.
 */
public class MotionDetectorJavaCVDebugRenderer {
    final private MotionProcessorJavaCV motionProcessor;
    final private Size windowSize;

    final Thread owner;

    public MotionDetectorJavaCVDebugRenderer(
            MotionProcessorJavaCV motionProcessor, Size windowSize) {
        this.motionProcessor = motionProcessor;
        this.windowSize = windowSize;
        this.owner = Thread.currentThread();
    }

    public void render(Mat debugOutput, Rect r,
            Map<Presence, Rect> presenceIndicators, Set<Presence> indicators,
            boolean contourMotionDetected, boolean trackerMotionDetected,
            double fps) {
        if (Thread.currentThread() != owner) {
            throw new ConcurrentModificationException(owner.toString() + "!="
                    + Thread.currentThread().toString());
        }
        boolean present = indicators.contains(Presence.Present);
        // Motion
        if (indicators.contains(Presence.Shake)) {
            Rect presenceRect = presenceIndicators.get(Presence.Present);
            rectangle(debugOutput, presenceRect,
                    present ? Color.MidBlue : Color.DarkBlue, 15, 8, 0);
            presenceRect.close();
        } else {
            if (r != null) {
                renderMotionRegion(debugOutput, r, present);
            }
            renderPresenceIndicators(debugOutput, r, presenceIndicators,
                    indicators, present);
            motionProcessor.trackFeatures.render(debugOutput, Green);
            // tracker distance
            if (contourMotionDetected && !trackerMotionDetected) {
                renderContourMotionRegion(debugOutput, r);
            } else if (trackerMotionDetected) {
                renderDistanceTrackerPoints(debugOutput);
            }
        }
        renderRegionList(debugOutput, indicators);
        renderFPS(debugOutput, fps);
    }

    private static void renderMotionRegion(Mat debugOutput, Rect r,
            boolean present) {
        drawRect(debugOutput, r, "", present ? Green : Blue);
        circle(debugOutput, center(r), 2, present ? Green : Blue, 2, 8, 0);
    }

    private void renderContourMotionRegion(Mat debugOutput, Rect rM) {
        motionProcessor.motionContours.render(debugOutput, -1, White);
        if (rM != null) {
            Point p = new Point(debugOutput.cols() - 40,
                    debugOutput.cols() - 20);
            putText(debugOutput, rM.area() + "p2", p, FONT_HERSHEY_PLAIN, 2.75,
                    White);
            p.close();
        }
    }

    private void renderDistanceTrackerPoints(Mat debugOutput) {
        if (motionProcessor.trackFeatures.haveFeatures()) {
            motionProcessor.distanceTracker.renderDebug(debugOutput,
                    motionProcessor.trackFeatures.keyPoints());
        }
    }

    private static void renderRegionList(Mat debugOutput,
            Set<Presence> indicators) {
        int n = 0;
        int s = 14;
        for (Presence indicator : indicators) {
            Point p = new Point(0, n);
            putText(debugOutput, indicator.toString(), p, FONT_HERSHEY_PLAIN,
                    1.25, White);
            n += s;
            p.close();
        }
    }

    private static void renderFPS(Mat debugOutput, double fps) {
        // fps
        String fpsFormatted = String.format("%1$.2f", fps);
        Point p = new Point(0, debugOutput.rows() - 10);
        putText(debugOutput, fpsFormatted + "fps", p, FONT_HERSHEY_PLAIN, 1.75,
                White);
        p.close();
    }

    private void renderPresenceIndicators(Mat debugOutput, Rect rM,
            Map<Presence, Rect> presenceIndicators, Set<Presence> indicators,
            boolean present) {
        // Presence indicators
        for (Map.Entry<Presence, Rect> entry : presenceIndicators.entrySet()) {
            if (indicators.contains(entry.getKey())) {
                if (entry.getKey() == Presence.Present) {
                    circle(debugOutput, center(rM), windowSize.height() / 4,
                            present ? MidGreen : MidBlue, 4, 4, 0);
                } else {
                    rectangle(debugOutput, entry.getValue(),
                            present ? DarkGreen : DarkBlue, 4, 8, 0);
                }
            }
        }
    }

    public void close() {
    }
}
