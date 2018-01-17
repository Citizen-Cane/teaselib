package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_video.*;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
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

    public void start(Mat videoImage) {
        start(videoImage, (Mat) null);
    }

    public void start(Mat videoImage, Rect rect) {
        Mat mask = Mat.zeros(videoImage.size(), CV_8UC1).asMat();

        Mat roi = new Mat(mask, rect);
        Scalar scalar = new Scalar(255, 255, 255, 255);
        roi.put(scalar);

        start(videoImage, mask);

        scalar.close();
        roi.close();
        mask.close();
    }

    public void start(Mat input, Mat mask) {
        cvtColor(input, videoMatGray, COLOR_BGRA2GRAY);
        videoMatGrayPrevious = new Mat(input.size(), opencv_core.CV_8UC1);

        Size size = new Size(maxPoints * 2, 2);
        keyPoints = new Mat(size, opencv_core.CV_8UC4);
        keyPointsPrevious = new Mat(size, opencv_core.CV_8UC4);

        double qualityLevel = 0.20;
        double minDistance = input.cols() / 40.0;

        if (mask != null) {
            goodFeaturesToTrack(videoMatGray, keyPoints, maxPoints, qualityLevel, minDistance, mask, 3, false, 0.04);
        } else {
            goodFeaturesToTrack(videoMatGray, keyPoints, maxPoints, qualityLevel, minDistance);
        }
        keyPoints.copyTo(keyPointsPrevious);
        size.close();
    }

    public void update(Mat input) {
        if (hasFeatures()) {
            swapPixels(input);
            swapKeyPoints();
            calcOpticalFlowPyrLK(videoMatGrayPrevious, videoMatGray, keyPointsPrevious, keyPoints, status, error);
        }
    }

    private void swapPixels(Mat input) {
        Mat swap = videoMatGrayPrevious;
        videoMatGrayPrevious = videoMatGray;
        videoMatGray = swap;
        cvtColor(input, videoMatGray, COLOR_BGRA2GRAY);
    }

    private void swapKeyPoints() {
        Mat swap = keyPointsPrevious;
        keyPointsPrevious = keyPoints;
        keyPoints = swap;
    }

    public boolean hasFeatures() {
        return keyPoints.rows() * keyPoints.cols() > 0;
    }

    public Mat previousKeyPoints() {
        return keyPointsPrevious;
    }

    public Mat keyPoints() {
        return keyPoints;
    }

    public void render(Mat input, Scalar color) {
        if (hasFeatures()) {
            FloatIndexer points = keyPoints.createIndexer();
            for (int i = 0; i < points.rows(); i++) {
                opencv_core.Point p = new opencv_core.Point((int) points.get(i, 0), (int) points.get(i, 1));
                int size = 15;
                circle(input, p, 1, color);
                circle(input, p, size, color);
                p.close();
            }
            points.release();

            try {
                points.close();
            } catch (Exception e) { //
            }
        }
    }
}
