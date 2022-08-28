package teaselib.hosts;

import static java.awt.geom.AffineTransform.getTranslateInstance;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

/**
 * @author Citizen-Cane
 *
 */
public class SceneRenderer {

    static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    final Image backgroundImage;

    final BufferedImageQueue surfaces = new BufferedImageQueue(2);
    // final VolatileImageQueue textOverlays = new VolatileImageQueue(2);
    final BufferedImageQueue textOverlays = new BufferedImageQueue(2);

    final TextRenderer textRenderer = new TextRenderer();

    public SceneRenderer(Image backgroundImage) {
        super();
        this.backgroundImage = backgroundImage;
    }

    void render(Graphics2D g2d, GraphicsConfiguration gc, RenderState frame, RenderState previousImage,
            Rectangle bounds, Color backgroundColor) {
        // Bicubic interpolation is an absolute performance killer for image transforming & scaling
        // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        boolean previousImageVisible = frame.isBackgroundVisisble();
        boolean backgroundVisible = previousImageVisible ||
                previousImage.isBackgroundVisisble();
        if (backgroundVisible) {
            g2d.setBackground(backgroundColor);
            g2d.clearRect(0, 0, bounds.width, bounds.height);
            g2d.drawImage(backgroundImage,
                    0, 0, bounds.width, bounds.height,
                    0, 0, backgroundImage.getWidth(null),
                    backgroundImage.getHeight(null) * bounds.height / bounds.width, null);
        }

        if (previousImageVisible) {
            updateSceneTransform(previousImage, bounds);
        }

        if (frame.repaintSceneImage) {
            updateSceneTransform(frame, bounds);
        }

        // Choose the smaller image for blending in order to hide as much of the background image as possible
        if (previousImage.actorZoom < frame.actorZoom) {
            drawImageStack(g2d, gc, frame, 1.0f - frame.sceneBlend, previousImage, bounds);
        } else {
            drawImageStack(g2d, gc, previousImage, frame.sceneBlend, frame, bounds);
        }

        if (frame.isIntertitle || previousImage.isIntertitle) {
            renderIntertitle(g2d, frame, previousImage, bounds);
        }

        if (frame.repaintTextImage) {
            frame.textImage.paint(gc, g2d2 -> {
                Optional<Rectangle2D> focusRegion = frame.pose.face();
                Optional<Rectangle> focusArea = focusRegion.isPresent()
                        ? Optional.of(focusPixelArea(frame, bounds, focusRegion))
                        : Optional.empty();
                textRenderer.drawText(g2d2, frame, bounds, focusArea);
            });
        }

        // Avoid both overlays rendered at the same time when not blending over
        if (frame.textBlend < 1.0) {
            drawTextOverlay(g2d, gc, previousImage);
        }
        drawTextOverlay(g2d, gc, frame);
    }

    private static Rectangle focusPixelArea(RenderState frame, Rectangle bounds, Optional<Rectangle2D> focusRegion) {
        Dimension image = frame.displayImage.dimension();
        var transform = surfaceTransform(image, bounds, 1.0, focusRegion, new Point2D.Double());
        return ImageRenderer.normalizedToGraphics(transform, image, focusRegion.get());
    }

    /**
     * @param g2d
     *            Graphics context
     * @param bottom
     *            Bottom image
     * @param alpha
     *            alpha blend factor
     * @param top
     *            Top image
     * @param bounds
     *            region
     */
    private static void drawImageStack(Graphics2D g2d, GraphicsConfiguration gc, RenderState bottom, float alpha,
            RenderState top, Rectangle bounds) {
        if (top.isBackgroundVisisble() || alpha < 1.0) {
            var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
            g2d.setComposite(alphaComposite);
            drawImage(g2d, gc, bottom);
            // ImageRenderer.drawDebugInfo(g2d, bottom, bounds);
        }

        if (alpha < 1.0f) {
            var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            g2d.setComposite(alphaComposite);
        }

        drawImage(g2d, gc, top);
        // ImageRenderer.drawDebugInfo(g2d, top, bounds);
    }

    private static void drawImage(Graphics2D g2d, GraphicsConfiguration gc, RenderState frame) {
        if (frame.displayImage != null) {
            if (frame.focusLevel < 1.0) {
                throw new UnsupportedOperationException("Blur-op not available for volatile images");
            } else {
                frame.displayImage.draw(g2d, gc, frame.transform);
            }
        }
    }

    private static void renderIntertitle(Graphics2D g2d, RenderState frame, RenderState previousImage,
            Rectangle bounds) {
        var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        g2d.setComposite(alphaComposite);
        renderIntertitleGlass(g2d, bounds, getIntertitleAlphaBlend(frame, previousImage));
    }

    private static void renderIntertitleGlass(Graphics2D g2d, Rectangle bounds, float alpha) {
        Rectangle centerRegion = TextRenderer.interTitleCenterRegion(bounds);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f * alpha));
        g2d.fillRect(bounds.x, bounds.y, bounds.width, centerRegion.y);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.80f * alpha));
        g2d.fillRect(centerRegion.x, centerRegion.y, centerRegion.width, centerRegion.height);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f * alpha));
        g2d.fillRect(bounds.x, centerRegion.y + centerRegion.height, bounds.width,
                bounds.height - centerRegion.height - centerRegion.y - bounds.y);
    }

    private static float getIntertitleAlphaBlend(RenderState frame, RenderState previousImage) {
        float alpha;
        if (frame.isIntertitle && previousImage.isIntertitle) {
            alpha = 1.0f;
        } else if (frame.isIntertitle) {
            alpha = frame.textBlend;
        } else {
            alpha = previousImage.textBlend;
        }
        return alpha;
    }

    private void drawTextOverlay(Graphics2D g2d, GraphicsConfiguration gc, RenderState frame) {
        if (!frame.text.isBlank()) {
            draw(g2d, gc, frame.textImage, frame.textBlend, frame.textImageRegion);
            // textRenderer.drawDebugInfo(g2d);
        }
    }

    void draw(Graphics2D g2d, GraphicsConfiguration gc, AbstractValidatedImage<?> text, float alpha,
            Rectangle textArea) {
        if (alpha > 0.0f) {
            var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            g2d.setComposite(alphaComposite);
            var clip = g2d.getClip();
            try {
                g2d.setClip(textArea);
                text.draw(g2d, gc);
            } finally {
                g2d.setClip(clip);
            }
        }
    }

    // TODO Add focus point update in order to control focus by animated host

    void updateSceneTransform(RenderState frame, Rectangle bounds) {
        if (frame.displayImage != null) {
            frame.transform = surfaceTransform(
                    frame.displayImage.dimension(),
                    bounds,
                    frame.actorZoom,
                    frame.focusRegion(),
                    frame.displayImageOffset);
        } else {
            frame.transform = null;
        }
    }

    private static AffineTransform surfaceTransform(Dimension image, Rectangle2D bounds, double zoom,
            Optional<Rectangle2D> focusRegion, Point2D displayImageOffset) {
        var surface = new AffineTransform();
        surface.concatenate(AffineTransform.getTranslateInstance(bounds.getMinX(), bounds.getMinY()));
        surface.concatenate(Transform.maxImage(image, bounds, focusRegion));
        if (focusRegion.isPresent()) {
            Rectangle2D imageFocusArea = Transform.scale(focusRegion.get(), image);
            surface = Transform.matchGoldenRatioOrKeepVisible(surface, image, bounds, imageFocusArea);
            surface.concatenate(Transform.zoom(imageFocusArea, zoom));
        }
        surface.preConcatenate(getTranslateInstance(displayImageOffset.getX(), displayImageOffset.getY()));
        return surface;
    }

}
