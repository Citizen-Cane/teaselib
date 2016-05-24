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
import teaselib.motiondetection.MotionDetector.Presence;

/**
 * OpenCV imshow(...) isn't thread safe, it may hang (at least on windows) if
 * called from several threads.
 */
public class MotionDetectorJavaCVDebugRenderer {
    private static final String INPUT = "Input";
    private static final String MOTION = "Motion";

    final private MotionProcessorJavaCV motionProcessor;
    final private Size windowSize;
    final private Mat debugOutput = new Mat();

    final Thread owner;

    public MotionDetectorJavaCVDebugRenderer(
            MotionProcessorJavaCV motionProcessor, Size windowSize) {
        this.motionProcessor = motionProcessor;
        this.windowSize = windowSize;
        this.owner = Thread.currentThread();

        String windows[] = { INPUT, MOTION };
        positionWindows(windowSize.width(), windowSize.height(), windows);
    }

    public void render(Mat input, Rect r,
            Map<Presence, Rect> presenceIndicators, Set<Presence> indicators,
            boolean contourMotionDetected, boolean trackerMotionDetected,
            double fps) {
        if (Thread.currentThread() != owner) {
            throw new ConcurrentModificationException(owner.toString() + "!="
                    + Thread.currentThread().toString());
        }
        // Copy source mat because when the video capture device
        // framerate drops below the motion detection frame rate,
        // the debug renderer would mess up motion detection
        // when rendering into the source mat
        input.copyTo(debugOutput);
        boolean present = indicators.contains(Presence.Present);
        // Motion
        if (indicators.contains(Presence.Shake)) {
            @SuppressWarnings("resource")
            Rect presenceRect = presenceIndicators.get(Presence.Present);
            rectangle(debugOutput, presenceRect,
                    present ? Color.MidBlue : Color.DarkBlue, 15, 8, 0);
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
        updateWindows(debugOutput);
    }

    private static void renderMotionRegion(Mat debugOutput, Rect r,
            boolean present) {
        drawRect(debugOutput, r, "", present ? Green : Blue);
        circle(debugOutput, center(r), 2, present ? Green : Blue, 2, 8, 0);
    }

    private void renderContourMotionRegion(Mat debugOutput, Rect rM) {
        motionProcessor.motionContours.render(debugOutput, DarkRed, -1);
        if (rM != null) {
            putText(debugOutput, rM.area() + "p2",
                    new Point(debugOutput.cols() - 40, debugOutput.cols() - 20),
                    FONT_HERSHEY_PLAIN, 2.75, White);
        }
    }

    private void renderDistanceTrackerPoints(Mat debugOutput) {
        if (motionProcessor.trackFeatures.haveFeatures()) {
            motionProcessor.distanceTracker.renderDebug(debugOutput,
                    motionProcessor.trackFeatures.keyPoints());
        }
    }

    private void updateWindows(Mat debugOutput) {
        org.bytedeco.javacpp.opencv_highgui.imshow(INPUT, debugOutput);
        org.bytedeco.javacpp.opencv_highgui.imshow(MOTION,
                motionProcessor.motion.output);
        if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
            // break;
        }
    }

    private void renderRegionList(Mat debugOutput, Set<Presence> indicators) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        int s = 14;
        for (Presence indicator : indicators) {
            putText(debugOutput, indicator.toString(), new Point(0, n),
                    FONT_HERSHEY_PLAIN, 1.25, White);
            n += s;
        }
    }

    private void renderFPS(Mat debugOutput, double fps) {
        // fps
        String fpsFormatted = String.format("%1$.2f", fps);
        putText(debugOutput, fpsFormatted + "fps",
                new Point(0, debugOutput.rows() - 10), FONT_HERSHEY_PLAIN, 1.75,
                White);
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
}
