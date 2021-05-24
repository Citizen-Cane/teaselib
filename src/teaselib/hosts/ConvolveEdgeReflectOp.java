package teaselib.hosts;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;

public class ConvolveEdgeReflectOp implements BufferedImageOp, RasterOp {
    private final ConvolveOp convolve;

    public static BufferedImageOp blur(int n) {
        return new ConvolveEdgeReflectOp(blurKernel(n));
    }

    private static Kernel blurKernel(int n) {
        int size = n * n;
        float nth = 1.0f / size;
        var data = new float[size];
        for (int i = 0; i < size; i++) {
            data[i] = nth;
        }
        return new Kernel(n, n, data);
    }

    public ConvolveEdgeReflectOp(Kernel kernel) {
        this.convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }

    @Override
    public BufferedImage filter(BufferedImage source, BufferedImage destination) {
        var kernel = convolve.getKernel();
        int borderWidth = kernel.getWidth() / 2;
        int borderHeight = kernel.getHeight() / 2;

        BufferedImage original = addBorder(source, borderWidth, borderHeight);
        return convolve.filter(original, destination) //
                .getSubimage(borderWidth, borderHeight, source.getWidth(), source.getHeight());
    }

    private static BufferedImage addBorder(BufferedImage image, int borderWidth, int borderHeight) {
        int w = image.getWidth();
        int h = image.getHeight();

        var cm = image.getColorModel();
        var raster = cm.createCompatibleWritableRaster(w + 2 * borderWidth, h + 2 * borderHeight);
        var bordered = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), null);
        Graphics2D g = bordered.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g.drawImage(image, borderWidth, borderHeight, null);
            g.drawImage(image, borderWidth, 0, borderWidth + w, borderHeight, 0, 0, w, 1, null);
            g.drawImage(image, -w + borderWidth, borderHeight, borderWidth, h + borderHeight, 0, 0, 1, h, null);
            g.drawImage(image, w + borderWidth, borderHeight, 2 * borderWidth + w, h + borderHeight, w - 1, 0, w, h,
                    null);
            g.drawImage(image, borderWidth, borderHeight + h, borderWidth + w, 2 * borderHeight + h, 0, h - 1, w, h,
                    null);
        } finally {
            g.dispose();
        }

        return bordered;
    }

    @Override
    public WritableRaster filter(Raster src, WritableRaster dst) {
        return convolve.filter(src, dst);
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        return convolve.createCompatibleDestImage(src, destCM);
    }

    @Override
    public WritableRaster createCompatibleDestRaster(Raster src) {
        return convolve.createCompatibleDestRaster(src);
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return convolve.getBounds2D(src);
    }

    @Override
    public Rectangle2D getBounds2D(Raster src) {
        return convolve.getBounds2D(src);
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return convolve.getPoint2D(srcPt, dstPt);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return convolve.getRenderingHints();
    }

}
