package teaselib.host;

import static java.awt.geom.AffineTransform.*;

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

import teaselib.host.Transform.FitFunction;
import teaselib.host.Transform.FocusFunction;

/**
 * @author Citizen-Cane
 *
 */
public class SceneRenderer {

    static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    record View(FitFunction fit, FocusFunction focus) {
        static final View FitOutside = new View(Transform::fitOutside, Transform::matchGoldenRatioOrKeepVisible);
        static final View FitAspected = new View(Transform::fitAspected, Transform::matchFocusRegion);
    }

    final View view = View.FitAspected;

    final Image backgroundImage;
    public final BufferedImageQueue surfaces;
    // final VolatileImageQueue textOverlays;
    public final BufferedImageQueue textOverlays;

    final TextRenderer textRenderer = new TextRenderer();

    public SceneRenderer(Image backgroundImage, int surfaceBuffers) {
        this.backgroundImage = backgroundImage;
        this.surfaces = new BufferedImageQueue(surfaceBuffers);
        // this.textOverlays = new VolatileImageQueue(2);
        this.textOverlays = new BufferedImageQueue(2);
    }

    public void render(Graphics2D g2d, GraphicsConfiguration gc, RenderState frame, RenderState previousImage, Rectangle bounds, Color backgroundColor) {
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

        var clip = g2d.getClip();
        var r = createBlendClip(frame, previousImage, bounds);
        if (r != null) {
            g2d.setClip(r.x, r.y, r.width, r.height);
        }
        try {
            drawScene(g2d, gc, frame, previousImage);
            // ImageRenderer.drawDebugInfo(g2d, previousImage, bounds);
            // ImageRenderer.drawDebugInfo(g2d, frame, bounds);
        } finally {
            if (r != null) {
                g2d.setClip(clip);
            }
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

    private Rectangle createBlendClip(RenderState current, RenderState previous, Rectangle bounds) {
        Rectangle r1 = getBlendClipRect(current, bounds, current.actorZoom);
        Rectangle r2 = getBlendClipRect(previous, bounds, 1.0);
        if (r1 == null)
            return r2;
        if (r2 == null)
            return r1;
        return r1.union(r2);
    }

    private Rectangle getBlendClipRect(RenderState renderState, Rectangle bounds, double zoom) {
        var image = renderState.displayImage;
        if (image != null) {
            var t = surfaceTransform(
                    image.dimension(),
                    bounds,
                    zoom,
                    renderState.focusRegion(),
                    new Point2D.Double(0.0, 0.0));
            return Transform.transform(t, 0.0, 0.0, image.getWidth(), image.getHeight());
        } else {
            return null;
        }
    }

    private static void drawScene(Graphics2D g2d, GraphicsConfiguration gc, RenderState frame, RenderState previousImage) {
        if (previousImage.sceneBlend > 0.0f) {
            // Choose the smaller image for blending in order to hide as much of the background image as possible
            if (previousImage.actorZoom < frame.actorZoom) {
                drawImageStack(g2d, gc, frame, 1.0f - frame.sceneBlend, previousImage);
            } else {
                drawImageStack(g2d, gc, previousImage, frame.sceneBlend, frame);
            }
        } else {
            drawImage(g2d, gc, frame);
        }
    }

    private static void drawImageStack(Graphics2D g2d, GraphicsConfiguration gc, RenderState bottom, float alpha, RenderState top) {
        if (top.isBackgroundVisisble() || alpha < 1.0) {
            var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
            g2d.setComposite(alphaComposite);
            drawImage(g2d, gc, bottom);
        }

        if (alpha < 1.0f) {
            var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            g2d.setComposite(alphaComposite);
        }

        drawImage(g2d, gc, top);
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

    private Rectangle focusPixelArea(RenderState frame, Rectangle bounds, Optional<Rectangle2D> focusRegion) {
        Dimension image = frame.displayImage.dimension();
        var transform = surfaceTransform(image, bounds, 1.0, focusRegion, new Point2D.Double());
        return ImageRenderer.normalizedToGraphics(transform, image, focusRegion.get());
    }

    public Point2D focusPoint(RenderState renderState, Rectangle bounds, Point2D focus) {
        updateSceneTransform(renderState, bounds);
        return renderState.transform.transform(Transform.scale(focus, renderState.displayImage.dimension()), new Point2D.Double());
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

    private static void drawTextOverlay(Graphics2D g2d, GraphicsConfiguration gc, RenderState frame) {
        if (!frame.text.isBlank()) {
            draw(g2d, gc, frame.textImage, frame.textBlend, frame.textImageRegion);
            // textRenderer.drawDebugInfo(g2d);
        }
    }

    private static void draw(Graphics2D g2d, GraphicsConfiguration gc, AbstractValidatedImage<?> text, float alpha, Rectangle textArea) {
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

    private void updateSceneTransform(RenderState frame, Rectangle bounds) {
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

    private AffineTransform surfaceTransform(Dimension image, Rectangle2D bounds, double zoom,
            Optional<Rectangle2D> focusRegion, Point2D displayImageOffset) {
        var surface = new AffineTransform();
        surface.concatenate(AffineTransform.getTranslateInstance(bounds.getMinX(), bounds.getMinY()));
        surface.concatenate(Transform.maxImage(image, bounds, focusRegion, view.fit));
        if (focusRegion.isPresent()) {
            Rectangle2D imageFocusArea = Transform.scale(focusRegion.get(), image);
            surface = view.focus.apply(surface, image, bounds, imageFocusArea);
            surface.concatenate(Transform.zoom(imageFocusArea, zoom));
        }
        surface.preConcatenate(getTranslateInstance(displayImageOffset.getX(), displayImageOffset.getY()));
        return surface;
    }

}
