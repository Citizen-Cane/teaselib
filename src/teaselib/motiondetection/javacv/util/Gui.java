package teaselib.motiondetection.javacv.util;

import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_imgproc.approxPolyDP;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;

public class Gui {
    public static void positionWindows(int width, int height,
            String[] windows) {
        for (String name : windows) {
            org.bytedeco.javacpp.opencv_highgui.namedWindow(name);
        }
        int space = 20;
        int x = 80;
        // int y = 1340 - height * 2 - space * 2;
        int y = 1440 - height * 2 - space * 2;
        org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[0], x, y);
        if (windows.length >= 2) {
            org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[1],
                    x + width + space, y);
            if (windows.length >= 3) {
                org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[2], x,
                        y + height + space * 2);
                if (windows.length >= 4) {
                    org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[3],
                            x + width + space, y + height + space * 2);
                }
            }
        }
    }

    public static void drawRect(Mat mat, Rect r, Scalar color) {
        rectangle(mat, r, color);
    }

    public static void drawRect(Mat mat, Rect r, String name, Scalar color) {
        drawRect(mat, r, color);
        // Create the text we will annotate the box with:
        String box_text = "<" + name + ">";
        // Calculate the position for annotated text (make sure we don't
        // put illegal values in there):
        int pos_x = Math.max(r.tl().x() + 10, 0);
        int pos_y = Math.max(r.tl().y() + 10, 0);
        // And now put it into the image:
        putText(mat, box_text, new Point(pos_x, pos_y), FONT_HERSHEY_PLAIN,
                1.75, color);
    }

    /**
     * @param mat
     * @param color
     * @param size
     */
    public static void rectangles(Mat mat, Collection<Rect> rectangles,
            Scalar color, int size) {
        for (Rect r : rectangles) {
            rectangle(mat, r, color, size, 8, 0);
        }
    }

    public static List<Rect> rectangles(MatVector contours) {
        int size = (int) contours.size();
        List<Rect> rectangles = new ArrayList<Rect>(size);
        for (int i = 0; i < size; i++) {
            // TODO Remove approxPolyDP?
            Mat p = new Mat();
            approxPolyDP(contours.get(i), p, 3, true);
            Rect r = boundingRect(p);
            // Rect r = opencv_imgproc.boundingRect(contours.get(i));
            if (r != null)
                rectangles.add(r);
        }
        return rectangles;
    }

}
