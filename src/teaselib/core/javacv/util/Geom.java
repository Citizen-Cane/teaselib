package teaselib.core.javacv.util;

import static org.bytedeco.javacpp.opencv_imgproc.approxPolyDP;
import static org.bytedeco.javacpp.opencv_imgproc.boundingRect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.indexer.IntIndexer;

import teaselib.util.math.Partition;
import teaselib.util.math.Statistics;

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
            return null;
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
        if (r != null) {
            if (a != null && b != null) {
                int x = Math.min(a.x(), b.x());
                int y = Math.min(a.y(), b.y());
                int width = Math.max(a.x() + a.width(), b.x() + b.width()) - x;
                int height = Math.max(a.y() + a.height(), b.y() + b.height())
                        - y;
                r.x(x);
                r.y(y);
                r.width(width);
                r.height(height);
            } else if (a != null && b == null) {
                r.x(a.x());
                r.y(a.y());
                r.width(a.width());
                r.height(a.height());
            } else if (a == null && b != null) {
                r.x(b.x());
                r.y(b.y());
                r.width(b.width());
                r.height(b.height());
            } else {
                throw new IllegalArgumentException();
            }
        } else
            throw new IllegalArgumentException();
    }

    public static List<Rect> rectangles(MatVector contours) {
        int size = (int) contours.size();
        List<Rect> rectangles = new ArrayList<Rect>(size);
        for (int i = 0; i < size; i++) {
            Mat p = new Mat();
            approxPolyDP(contours.get(i), p, 3, true);
            Rect r = boundingRect(p);
            if (r != null) {
                rectangles.add(r);
            }
        }
        return rectangles;
    }

    public static boolean isCircular(Mat contour, double circularity) {
        return isCircular(distance2Center(contour), circularity);
    }

    public static boolean isCircular(List<Integer> distance2Center,
            double circularity) {
        Collections.sort(distance2Center);
        Statistics statistics = new Statistics(distance2Center);
        int max = distance2Center.get(distance2Center.size() - 1);
        double mean = statistics.mean();
        double contourCircularity = max / mean;
        return contourCircularity <= circularity;
    }

    @SuppressWarnings("resource")
    private static List<Integer> distance2Center(Mat contour) {
        IntIndexer points = contour.createIndexer();
        int cx = 0;
        int cy = 0;
        final int s = points.rows();
        for (int i = 0; i < s; i++) {
            cx += points.get(i, 0);
            cy += points.get(i, 1);
        }
        cx /= s;
        cy /= s;
        Point center = new Point(cx, cy);
        List<Integer> distance2Center = new ArrayList<Integer>(s);
        for (int i = 0; i < s; i++) {
            int x = points.get(i, 0);
            int y = points.get(i, 1);
            opencv_core.Point p = new Point(x, y);
            distance2Center.add((int) Math.sqrt(distance2(center, p)));
        }
        // center.release();
        points.release();
        return distance2Center;
    }
}
