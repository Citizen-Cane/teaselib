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
import static teaselib.core.javacv.Color.DarkRed;
import static teaselib.core.javacv.Color.Green;
import static teaselib.core.javacv.Color.MidBlue;
import static teaselib.core.javacv.Color.MidGreen;
import static teaselib.core.javacv.Color.White;
import static teaselib.core.javacv.util.Geom.center;
import static teaselib.core.javacv.util.Gui.drawRect;
import static teaselib.core.javacv.util.Gui.positionWindows;

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
    final private Size size;

    final Thread owner;

    public MotionDetectorJavaCVDebugRenderer(
            MotionProcessorJavaCV motionProcessor, Size windowSize) {
        this.motionProcessor = motionProcessor;
        this.size = windowSize;
        this.owner = Thread.currentThread();

        String windows[] = { INPUT, MOTION };
        positionWindows(windowSize.width(), windowSize.height(), windows);
    }

    public void render(Mat videoImage, Rect r,
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
            rectangle(videoImage, presenceRect,
                    present ? Color.MidBlue : Color.DarkBlue, 15, 8, 0);
        } else {
            if (r != null) {
                renderMotionRegion(videoImage, r, present);
            }
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
        renderRegionList(indicators, videoImage);
        renderFPS(fps, videoImage);
        updateWindows(videoImage);
    }

    private static void renderMotionRegion(Mat videoImage, Rect r,
            boolean present) {
        drawRect(videoImage, r, "", present ? Green : Blue);
        circle(videoImage, center(r), 2, present ? Green : Blue, 2, 8, 0);
    }

    private void renderContourMotionRegion(Mat videoImage, Rect rM) {
        motionProcessor.motionContours.render(videoImage, DarkRed, -1);
        if (rM != null) {
            putText(videoImage, rM.area() + "p2",
                    new Point(videoImage.cols() - 40, videoImage.cols() - 20),
                    FONT_HERSHEY_PLAIN, 2.75, White);
        }
    }

    private void renderDistanceTrackerPoints(Mat videoImage) {
        if (motionProcessor.trackFeatures.haveFeatures()) {
            motionProcessor.distanceTracker.renderDebug(videoImage,
                    motionProcessor.trackFeatures.keyPoints());
        }
    }

    private void updateWindows(Mat videoImage) {
        org.bytedeco.javacpp.opencv_highgui.imshow(INPUT, videoImage);
        org.bytedeco.javacpp.opencv_highgui.imshow(MOTION,
                motionProcessor.motion.output);
        if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
            // break;
        }
    }

    private void renderRegionList(Set<Presence> indicators, Mat videoImage) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        int s = 12;
        for (Presence indicator : indicators) {
            putText(videoImage, indicator.toString(), new Point(0, n),
                    FONT_HERSHEY_PLAIN, 1.25, White);
            n += s;
        }
    }

    private void renderFPS(double fps, Mat videoImage) {
        // fps
        String fpsFormatted = String.format("%1$.2f", fps);
        putText(videoImage, fpsFormatted + "fps",
                new Point(0, videoImage.rows() - 10), FONT_HERSHEY_PLAIN, 1.75,
                White);
    }

    private void renderPresenceIndicators(Mat videoImage, Rect rM,
            Map<Presence, Rect> presenceIndicators, Set<Presence> indicators,
            boolean present) {
        // Presence indicators
        for (Map.Entry<Presence, Rect> entry : presenceIndicators.entrySet()) {
            if (indicators.contains(entry.getKey())) {
                if (entry.getKey() == Presence.Present) {
                    circle(videoImage, center(rM), size.height() / 4,
                            present ? MidGreen : MidBlue, 4, 4, 0);
                } else {
                    rectangle(videoImage, entry.getValue(),
                            present ? DarkGreen : DarkBlue, 4, 8, 0);
                }
            }
        }
    }
}
