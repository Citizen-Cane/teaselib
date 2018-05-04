package teaselib.core.javacv;

import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGRA2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.circle;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.goodFeaturesToTrack;
import static org.bytedeco.javacpp.opencv_video.calcOpticalFlowPyrLK;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.indexer.FloatIndexer;

import teaselib.core.util.ExceptionUtil;

public class TrackFeatures {
    static final int MaxPoints = 256;
    static final Size FeatureSize = new Size(MaxPoints * 2, 2);
    static final Size StatusSize = new Size(MaxPoints, 1);

    private static final Mat EMPTY = new Mat(0, opencv_core.CV_8UC4);

    private static final Scalar maskPixel = new Scalar(255, 255, 255, 255);

    final Mat keyPointFeatures = new Mat(FeatureSize, opencv_core.CV_8UC4);
    final Mat keyPointsFeaturesPrevious = new Mat(FeatureSize, opencv_core.CV_8UC4);

    final Mat status = new Mat(StatusSize, CV_8UC1);
    final Mat error = new Mat(StatusSize, CV_8UC1);

    Mat videoMatGray = new Mat();
    Mat videoMatGrayPrevious = new Mat();

    Mat keyPoints = EMPTY;
    Mat keyPointsPrevious = EMPTY;

    public void start(Mat videoImage) {
        start(videoImage, (Mat) null);
    }

    public void start(Mat videoImage, Rect rect) {
        try (Mat mask = Mat.zeros(videoImage.size(), CV_8UC1).asMat(); Mat roi = new Mat(mask, rect);) {
            roi.put(maskPixel);
            start(videoImage, mask);
        }
    }

    public void start(Mat input, Mat mask) {
        cvtColor(input, videoMatGray, COLOR_BGRA2GRAY);
        videoMatGrayPrevious = new Mat(input.size(), opencv_core.CV_8UC1);

        keyPoints = keyPointFeatures;
        keyPointsPrevious = keyPointsFeaturesPrevious;

        double qualityLevel = 0.20;
        double minDistance = input.cols() / 40.0;

        if (mask != null) {
            goodFeaturesToTrack(videoMatGray, keyPoints, MaxPoints, qualityLevel, minDistance, mask, 3, false, 0.04);
        } else {
            goodFeaturesToTrack(videoMatGray, keyPoints, MaxPoints, qualityLevel, minDistance);
        }
        keyPoints.copyTo(keyPointsPrevious);
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
        return keyPoints != EMPTY && keyPoints.rows() * keyPoints.cols() > 0;
    }

    public Mat previousKeyPoints() {
        return keyPointsPrevious;
    }

    public Mat keyPoints() {
        return keyPoints;
    }

    public void render(Mat input, Scalar colorInside, Rect mask, Scalar colorOutside) {
        if (hasFeatures()) {
            render(input, colorInside, keyPoints, mask, colorOutside);
        }
    }

    public static void render(Mat input, Scalar colorInside, Mat keyPoints, Rect mask, Scalar colorOutside) {
        try (FloatIndexer points = keyPoints.createIndexer();) {
            for (int i = 0; i < points.rows(); i++) {
                try (opencv_core.Point p = new opencv_core.Point((int) points.get(i, 0), (int) points.get(i, 1));) {
                    Scalar color = mask == null || mask.contains(p) ? colorInside : colorOutside;
                    if (color != null) {
                        int size = 15;
                        circle(input, p, 1, color);
                        circle(input, p, size, color);
                    }
                }
            }
            points.release();
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public void clear() {
        keyPoints = EMPTY;
        keyPointsPrevious = EMPTY;
    }
}
