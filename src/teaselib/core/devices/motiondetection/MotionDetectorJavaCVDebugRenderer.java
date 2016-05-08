/**
 * 
 */
package teaselib.core.devices.motiondetection;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static teaselib.core.javacv.Color.*;
import static teaselib.core.javacv.util.Geom.*;
import static teaselib.core.javacv.util.Gui.*;

import java.util.EnumSet;
import java.util.Map;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Size;

import teaselib.motiondetection.MotionDetector.Presence;

public class MotionDetectorJavaCVDebugRenderer {
    private static final String INPUT = "Input";
    private static final String MOTION = "Motion";

    final private MotionProcessorJavaCV motionProcessor;
    final private Size size;

    public MotionDetectorJavaCVDebugRenderer(
            MotionProcessorJavaCV motionProcessor, Size windowSize) {
        this.motionProcessor = motionProcessor;
        this.size = windowSize;
        String windows[] = { INPUT, MOTION };
        positionWindows(windowSize.width(), windowSize.height(), windows);
    }

    public void render(Mat videoImage, Rect r,
            Map<Presence, Rect> presenceIndicators,
            EnumSet<Presence> indicators, boolean contourMotionDetected,
            boolean trackerMotionDetected, double fps) {
        if (r != MotionProcessorJavaCV.None) {
            boolean present = indicators.contains(Presence.Present);
            // Motion
            if (indicators.contains(Presence.Shake)) {
                Rect presenceRect = presenceIndicators.get(Presence.Present);
                rectangle(videoImage, presenceRect,
                        present ? MidBlue : DarkBlue, 15, 8, 0);
            } else {
                renderMotionRegion(videoImage, r, present);
                renderPresenceIndicators(videoImage, r, presenceIndicators,
                        indicators, present);
                motionProcessor.trackFeatures.render(videoImage, Green);
                // tracker distance
                if (contourMotionDetected && !trackerMotionDetected) {
                    renderContourMotionRegion(videoImage, r);
                } else if (trackerMotionDetected) {
                    renderDistanceTrackerPoints(videoImage);
                }
            }
            renderFPS(fps, videoImage);
            updateWindows(videoImage);
        }
    }

    private static void renderMotionRegion(Mat videoImage, Rect r,
            boolean present) {
        drawRect(videoImage, r, "", present ? Green : Blue);
        circle(videoImage, center(r), 2, present ? Green : Blue, 2, 8, 0);
    }

    private void renderContourMotionRegion(Mat videoImage, Rect rM) {
        motionProcessor.motionContours.render(videoImage, DarkRed, -1);
        putText(videoImage, rM.area() + "p2",
                new Point(videoImage.cols() - 40, videoImage.cols() - 20),
                FONT_HERSHEY_PLAIN, 2.75, White);
    }

    private void renderDistanceTrackerPoints(Mat videoImage) {
        if (motionProcessor.trackFeatures.haveFeatures()) {
            motionProcessor.distanceTracker.renderDebug(videoImage,
                    motionProcessor.trackFeatures.keyPoints());
        }
    }

    private void updateWindows(Mat videoImage) {
        // org.bytedeco.javacpp.opencv_highgui.imshow(INPUT, videoImage);
        org.bytedeco.javacpp.opencv_highgui.imshow(MOTION,
                motionProcessor.motion.output);
        if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
            // break;
        }
    }

    private void renderFPS(double fps, Mat videoImage) {
        // fps
        String fpsFormatted = String.format("%1$.2f", fps);
        putText(videoImage, fpsFormatted + "fps", new Point(0, 40),
                FONT_HERSHEY_PLAIN, 2.75, White);
    }

    private void renderPresenceIndicators(Mat videoImage, Rect rM,
            Map<Presence, Rect> presenceIndicators,
            EnumSet<Presence> indicators, boolean present) {
        // Presence indicators
        for (Map.Entry<Presence, Rect> entry : presenceIndicators.entrySet()) {
            if (indicators.contains(entry.getKey())) {
                if (entry.getKey() == Presence.Present) {
                    circle(videoImage, center(rM), size.height() / 4,
                            present ? Green : Blue, 4, 4, 0);
                } else {
                    rectangle(videoImage, entry.getValue(),
                            present ? Red : Blue, 4, 8, 0);
                }
            }
        }
    }

}
