package teaselib.core.javacv.util;

import static org.bytedeco.javacpp.opencv_imgproc.approxPolyDP;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;

import teaselib.util.math.Partition;

public class Geom {
    public static List<Partition<Rect>.Group> partition(List<Rect> rectangles,
            int distance) {
        final int distance2 = distance * distance;
        Partition.Members<Rect> members = new Partition.Members<Rect>() {
            @Override
            public boolean similar(Rect rect1, Rect rect2) {
                return distance2(rect1, rect2) < distance2;
            }
        };
        Partition.Order<Rect> order = new Partition.Order<Rect>() {
            @Override
            public Rect group(Rect rect1, Rect rect2) {
                Rect group = new Rect(rect1);
                join(group, rect2, group);
                return group;
            }
        };
        Comparator<Rect> comperator = new Comparator<Rect>() {
            @Override
            public int compare(Rect r1, Rect r2) {
                return r1.area() - r2.area();
            }
        };
        Partition<Rect> partition = new Partition<Rect>(rectangles, members,
                order, comperator);
        return partition.groups;
    }

    public static Point center(List<Rect> rectangles) {
        return center(join(rectangles));
    }

    public static int distance2(Point p1, Point p2) {
        int x = p1.x() - p2.x();
        int y = p1.y() - p2.y();
        return x * x + y * y;
    }

    public static int distance2(Rect rect1, Rect rect2) {
        Point p1 = center(rect1);
        int r1 = (rect1.width() + rect1.height()) / 4;
        Point p2 = center(rect2);
        int r2 = (rect2.width() + rect2.height()) / 4;
        return distance2(p1, p2) - (r1 + r2) * (r1 + r2);
    }

    public static Point center(Rect r) {
        return new Point(r.x() + r.width() / 2, r.y() + r.height() / 2);
    }

    public static boolean intersects(Rect r1, Rect r2) {
        return r1.contains(r2.tl()) || r1.contains(r2.br())
                || r2.contains(r1.tl()) || r2.contains(r1.br());
    }

    public static Rect join(Collection<Rect> rectangles) {
        long size = rectangles.size();
        if (size == 0) {
            return new Rect();
        } else {
            Iterator<Rect> iterator = rectangles.iterator();
            Rect r = new Rect(iterator.next());
            for (; iterator.hasNext();) {
                join(r, iterator.next(), r);
            }
            return r;
        }
    }

    public static void join(Rect a, Rect b, Rect r) {
        int left = Math.min(a.x(), b.x());
        int top = Math.min(a.y(), b.y());
        int right = Math.max(a.x() + a.width(), b.x() + b.width());
        int bottom = Math.max(a.y() + a.height(), b.y() + b.height());
        r.x(left);
        r.y(top);
        r.width(right - left);
        r.height(bottom - top);
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
