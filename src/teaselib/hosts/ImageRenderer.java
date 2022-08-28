package teaselib.hosts;

import static java.awt.Color.*;
import static java.awt.geom.AffineTransform.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import teaselib.core.ai.perception.HumanPose;

/**
 * @author Citizen-Cane
 *
 */
class ImageRenderer {

    static void drawDebugInfo(Graphics2D g2d, RenderState frame, Rectangle bounds) {
        if (frame.displayImage != null) {
            drawDebugInfo(g2d, frame.displayImage, frame.pose, frame.transform, bounds);
        }
    }

    private static void drawDebugInfo(Graphics2D g2d, AbstractValidatedImage<?> image, HumanPose.Estimation pose,
            AffineTransform surface, Rectangle bounds) {
        drawBackgroundImageIconVisibleBounds(g2d, bounds);
        drawImageBounds(g2d, image.dimension(), surface);
        if (pose != HumanPose.Estimation.NONE) {
            drawPosture(g2d, image.dimension(), pose, surface);
        }
        // drawPixelGrid(g2d, bounds);
    }

    private static void drawBackgroundImageIconVisibleBounds(Graphics2D g2d, Rectangle bounds) {
        g2d.setColor(Color.green);
        g2d.drawRect(0, 0, bounds.width - 1, bounds.height - 1);
    }

    private static void drawImageBounds(Graphics2D g2d, Dimension image, AffineTransform surface) {
        g2d.setColor(Color.red);
        Point2D p0 = surface.transform(new Point2D.Double(0.0, 0.0), new Point2D.Double());
        Point2D p1 = surface.transform(new Point2D.Double(image.getWidth(), image.getHeight()), new Point2D.Double());
        g2d.drawRect(
                (int) p0.getX(), (int) p0.getY(), (int) (p1.getX() - p0.getX()) - 1,
                (int) (p1.getY() - p0.getY()) - 1);
    }

    private static void drawPosture(Graphics2D g2d, Dimension image, HumanPose.Estimation pose,
            AffineTransform surface) {
        if (pose.head.isPresent()) {
            var face = pose.face();
            Point2D poseHead = pose.head.get();
            Point2D p = surface.transform(
                    new Point2D.Double(poseHead.getX() * image.getWidth(), poseHead.getY() * image.getHeight()),
                    new Point2D.Double());
            int radius = face.isPresent() ? (int) (image.getWidth() * face.get().getWidth() / 3.0f) : 2;
            g2d.setColor(face.isPresent() ? cyan : orange);
            g2d.drawOval((int) p.getX() - 2, (int) p.getY() - 2, 2 * 2, 2 * 2);
            g2d.setColor(face.isPresent() ? cyan.darker().darker() : pink);
            g2d.drawOval((int) p.getX() - radius, (int) p.getY() - radius, 2 * radius, 2 * radius);
        }

        pose.face().ifPresent(region -> drawRegion(g2d, image, surface, region));
        pose.boobs().ifPresent(region -> drawRegion(g2d, image, surface, region));
    }

    private static void drawRegion(Graphics2D g2d, Dimension image, AffineTransform surface, Rectangle2D region) {
        var r = normalizedToGraphics(surface, image, region);
        g2d.setColor(Color.blue);
        g2d.drawRect(r.x, r.y, r.width, r.height);
    }

    static Rectangle normalizedToGraphics(AffineTransform surface, Dimension image, Rectangle2D region) {
        var scale = getScaleInstance(image.getWidth(), image.getHeight());
        var rect = scale.createTransformedShape(region);
        return surface.createTransformedShape(rect).getBounds();
    }

    static void drawPixelGrid(Graphics2D g2d, Rectangle bounds) {
        g2d.setColor(Color.BLACK);
        for (int x = 1; x < bounds.width - 1; x += 2) {
            g2d.drawLine(x, 1, x, bounds.height - 2);
        }
        for (int y = 1; y < bounds.height - 1; y += 2) {
            g2d.drawLine(1, y, bounds.width - 2, y);
        }
    }

}
