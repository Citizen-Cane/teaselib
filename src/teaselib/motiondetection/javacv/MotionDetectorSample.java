package teaselib.motiondetection.javacv;

import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static teaselib.motiondetection.javacv.util.Geom.center;
import static teaselib.motiondetection.javacv.util.Gui.drawRect;
import static teaselib.motiondetection.javacv.util.Gui.positionWindows;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;

/**
 * Detect presence and movement of person.
 * 
 * Presence detecting doesn't work, it's just assuming too much. It might work
 * while moving, but not under bad lighting conditions.
 * 
 * Motion detection works good, the structuring element can be used to control
 * sensitivity There is also the notion of a position, although it's not
 * accurate due to the limited history
 * 
 * However, the longer the history, the longer the lag until motion is reported
 * as stopped.
 * 
 * Overall, we can detect motion as well as the exact region, but if motion is
 * stopped, position and region values are very inaccurate.
 * 
 * Failure of presence detection is caused by lighting shadows if the room is
 * not lighted perfectly.
 * 
 * To accurately capture motion, the structuring element size should be derived
 * from the pixel size.
 * <p>
 * 
 */
public class MotionDetectorSample {

    private static final String INPUT = "Input";
    private static final String MOTION = "Motion";

    public static void main(String[] args) throws Exception {
        // String fileName = "30 Minute Intermediate Yoga Workout _ Juicy Heart
        // Opening Yoga Flow _ Joy _ Love _ Ecstatic Play.mp4";
        String fileName = "Yoga Poses_ Crow.mp4"; // front face
        // face
        // MovieFrameProvider movieFrameProvider = new
        // MovieFrameProvider(fileName, 2);
        VideoCaptureDeviceCV videoCaptureDevice = VideoCaptureDeviceCV.get(1);
        videoCaptureDevice.open(new Size(640, 480));

        // String windows[] = { INPUT, MOTION, PRESENSE, OUTPUT };
        String windows[] = { INPUT, MOTION };
        Size windowSize = videoCaptureDevice.size();
        positionWindows(windowSize.width(), windowSize.height(), windows);

        MotionProcessor motionDetector = new MotionProcessor(
                videoCaptureDevice.captureSize.width(),
                videoCaptureDevice.size.width());
        motionDetector.setStructuringElementSize(3);

        Scalar brightMotion = new Scalar(0, 0, 255, 0);
        for (Mat videoImage : videoCaptureDevice) {
            motionDetector.update(videoImage);
            // Resulting bounding boxes
            Rect rM = motionDetector.region();
            if (rM != MotionProcessor.None) {
                drawNumberedRect(videoImage,
                        motionDetector.motionContours.pixels(), rM,
                        motionDetector.pixels(), brightMotion);
                circle(videoImage, center(rM), 15, brightMotion, 12, 8, 0);
            }
            org.bytedeco.javacpp.opencv_highgui.imshow(INPUT, videoImage);
            org.bytedeco.javacpp.opencv_highgui.imshow(MOTION,
                    motionDetector.motion.output);
            if (org.bytedeco.javacpp.opencv_highgui.waitKey(30) >= 0) {
                break;
            }
        }
        videoCaptureDevice.close();
    }

    static void drawNumberedRect(Mat mat, int n, Rect r, int pixels,
            Scalar color) {
        drawRect(mat, r, Integer.toString(n) + ": " + pixels + "p2", color);
        circle(mat, center(r), 5, color);
    }

}
