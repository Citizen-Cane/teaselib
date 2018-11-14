package teaselib.core.javacv.util;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

import java.util.Collection;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;

public class Gui {

    private Gui() {
    }

    public static void positionWindows(int width, int height, String... windows) {
        for (String name : windows) {
            org.bytedeco.javacpp.opencv_highgui.namedWindow(name);
        }
        int space = 20;
        int x = 80;
        int y = 80;
        org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[0], x, y);
        if (windows.length >= 2) {
            org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[1], x + width + space, y);
            if (windows.length >= 3) {
                org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[2], x, y + height + space * 2);
                if (windows.length >= 4) {
                    org.bytedeco.javacpp.opencv_highgui.moveWindow(windows[3], x + width + space,
                            y + height + space * 2);
                }
            }
        }
    }

    public static void drawRect(Mat mat, Rect r, Scalar color) {
        rectangle(mat, r, color);
    }

    public static void drawRect(Mat mat, Rect r, String name, Scalar color) {
        drawRect(mat, r, color);
        if (name != null && name != "") {
            // Create the text we will annotate the box with:
            // Calculate the position for annotated text (make sure we don't
            // put illegal values in there):
            String boxText = "<" + name + ">";
            try (Point p = new Point(Math.max(r.tl().x() + 10, 0), Math.max(r.tl().y() + 10, 0));) {
                putText(mat, boxText, p, FONT_HERSHEY_PLAIN, 1.75, color);
            }
        }
    }

    /**
     * @param mat
     * @param color
     * @param size
     */
    public static void rectangles(Mat mat, Collection<Rect> rectangles, Scalar color, int size) {
        for (Rect r : rectangles) {
            rectangle(mat, r, color, size, 8, 0);
        }
    }
}
