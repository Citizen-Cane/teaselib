package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_video.*;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.FloatIndexer;

public class TrackFeatures {
    Mat videoMatGray = new Mat();
    Mat videoMatGrayPrevious = new Mat();
    Mat keyPoints = new Mat();
    Mat keyPointsPrevious = new Mat();

    int maxPoints = 256;
    Mat status = new Mat(new Size(maxPoints, 1), CV_8UC1);
    Mat error = new Mat(new Size(maxPoints, 1), CV_8UC1);

    public TrackFeatures() {
    }

    public void reset(Mat input, Mat mask) {
        cvtColor(input, videoMatGray, COLOR_BGRA2GRAY);
        cvtColor(input, videoMatGrayPrevious, COLOR_BGRA2GRAY);
        keyPoints = new Mat(new Size(maxPoints * 2, 2), opencv_core.CV_8UC4);
        if (mask != null) {
            keyPointsPrevious = new Mat(new Size(maxPoints * 2, 2),
                    opencv_core.CV_8UC4);
            goodFeaturesToTrack(videoMatGray, keyPoints, maxPoints, 0.5, 8.0,
                    mask, 3, false, 0.04);
        } else {
            keyPointsPrevious = new Mat(new Size(maxPoints * 2, 2),
                    opencv_core.CV_8UC4);
            goodFeaturesToTrack(videoMatGray, keyPoints, maxPoints, 0.5, 8.0);
        }
        keyPoints.copyTo(keyPointsPrevious);
        // update(input); // init current to be able to render something
    }

    public void update(Mat input) {
        cvtColor(input, videoMatGray, COLOR_BGRA2GRAY);
        if (haveFeatures()) {
            calcOpticalFlowPyrLK(videoMatGrayPrevious, videoMatGray,
                    keyPointsPrevious, keyPoints, status, error);
            swapBuffers(); // TODO points not tracked if placed before calc
            // opencv_video.calcOpticalFlowFarneback(arg0, arg1,
            // arg2,
            // arg3, arg4, arg5, arg6, arg7, arg8,
            // arg9);
        }
    }

    private void swapBuffers() {
        Mat swap = videoMatGrayPrevious;
        videoMatGrayPrevious = videoMatGray;
        videoMatGray = swap;
        swap = keyPointsPrevious;
        keyPointsPrevious = keyPoints;
        keyPoints = swap;
    }

    public boolean haveFeatures() {
        return keyPointsPrevious.rows() * keyPointsPrevious.cols() > 0;
    }

    public Mat keyPoints() {
        return keyPoints;
    }

    public void render(Mat input, Scalar color) {
        if (haveFeatures()) {
            FloatIndexer points = keyPoints.createIndexer();
            for (int i = 0; i < points.rows(); i++) {
                opencv_core.Point p = new opencv_core.Point(
                        (int) points.get(i, 0), (int) points.get(i, 1));
                int size = 5;
                circle(input, p, 1, color);
                circle(input, p, size, color);
                // p.close();
            }
            // color.close();
            points.release();
        }
    }
}
