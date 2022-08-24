package teaselib.hosts;

import static java.awt.Color.cyan;
import static java.awt.Color.orange;
import static java.awt.Color.pink;
import static java.awt.geom.AffineTransform.getScaleInstance;
import static java.awt.geom.AffineTransform.getTranslateInstance;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.Optional;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.ProximitySensor;
import teaselib.util.AnnotatedImage.Annotation;

/**
 * @author Citizen-Cane
 *
 */
public class BufferedImageRenderer {

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    final Image backgroundImage;

    final BufferedImageQueue surfaces = new BufferedImageQueue(2);
    // final VolatileImageQueue textOverlays = new VolatileImageQueue(2);
    final BufferedImageQueue textOverlays = new BufferedImageQueue(2);

    public BufferedImageRenderer(Image backgroundImage) {
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
                BufferedImageRenderer.renderText(g2d2, frame, bounds, focusRegion);
            });
        }

        // Avoid both overlays rendered at the same time when not blending over
        if (frame.textBlend < 1.0) {
            drawTextOverlay(g2d, gc, previousImage);
        }
        drawTextOverlay(g2d, gc, frame);
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
            // renderDebugInfo(g2d, bottom, bounds);
        }

        if (alpha < 1.0f) {
            var alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
            g2d.setComposite(alphaComposite);
        }

        drawImage(g2d, gc, top);
        // renderDebugInfo(g2d, top, bounds);
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
        renderIntertitle(g2d, bounds, getIntertitleAlphaBlend(frame, previousImage));
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
            drawTextOverlay(g2d, gc, frame.textImage, frame.textBlend, frame.textImageRegion);
        }
    }

    private static void drawTextOverlay(Graphics2D g2d, GraphicsConfiguration gc, AbstractValidatedImage<?> text,
            float alpha, Rectangle textArea) {
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

    public void updateSceneTransform(RenderState frame, Rectangle bounds) {
        if (frame.displayImage != null) {
            frame.transform = surfaceTransform(
                    frame.displayImage.dimension(),
                    bounds,
                    frame.actorZoom,
                    focusRegion(frame),
                    frame.displayImageOffset);
        } else {
            frame.transform = null;
        }
    }

    private static Optional<Rectangle2D> focusRegion(RenderState frame) {
        Optional<Rectangle2D> focusRegion;
        if (frame.annotations.contains(Annotation.Person.Actor)) {
            focusRegion = focusRegion(frame.pose, frame.actorZoom);
            if (focusRegion.isEmpty()) {
                focusRegion = Optional.of(new Rectangle2D.Double(0.4, 0.4, 0.2, 0.2));
            }
        } else if (frame.annotations.contains(Annotation.Person.Actor)) {
            // TODO images with non-actor models may be focused on as well,
            // probably only the whole body -> pose.bounds
            focusRegion = Optional.empty();
        } else {
            focusRegion = Optional.empty();
        }
        return focusRegion;
    }

    private static Optional<Rectangle2D> focusRegion(HumanPose.Estimation pose, double actorZoom) {
        if (actorZoom > ProximitySensor.zoom.get(Proximity.FACE2FACE)) {
            // TODO set focus area from proximity sensor to choose region depending on player state & position
            return pose.face(); // should be head -> boobs, or shoes -> shoes, etc.
        } else if (actorZoom > ProximitySensor.zoom.get(Proximity.AWAY)) {
            return pose.face();
        } else {
            return pose.face();
        }
    }

    static AffineTransform surfaceTransform(Dimension image, Rectangle2D bounds, double zoom,
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

    static void renderDebugInfo(Graphics2D g2d, RenderState frame, Rectangle bounds) {
        if (frame.displayImage != null) {
            renderDebugInfo(g2d, frame.displayImage, frame.pose, frame.transform, bounds);
        }
    }

    static void renderDebugInfo(Graphics2D g2d, AbstractValidatedImage<?> image, HumanPose.Estimation pose,
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

    private static Rectangle normalizedToGraphics(AffineTransform surface, Dimension image, Rectangle2D region) {
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

    //
    // =====================
    //

    /**
     * The minimum limits may cause text overflow but at small window size this looks more reasonable than incredibly
     * small font size.
     */
    class FontLimits {
        static final float MINIMAL_FONT_SIZE = 6.0f;
        static final int MINIMAL_TEXT_WIDTH = 100;
        static final int MINIMAL_TEXT_HEIGHT = 400;
    }

    private record TextInfo(Rectangle region, boolean rightAligned) { //
    }

    public static void renderText(Graphics2D g2d, RenderState frame, Rectangle bounds,
            Optional<Rectangle2D> focusRegion) {
        if (!frame.text.isBlank()) {
            Optional<Rectangle> focusArea = focusRegion.isPresent()
                    ? Optional.of(focusPixelArea(frame, bounds, focusRegion))
                    : Optional.empty();
            var textInfo = frame.isIntertitle ? intertitleTextArea(bounds) : spokenTextArea(bounds, focusArea);
            frame.textImageRegion = renderText(g2d, frame.text, textInfo, frame.isIntertitle);
        }
    }

    private static Rectangle focusPixelArea(RenderState frame, Rectangle bounds, Optional<Rectangle2D> focusRegion) {
        Dimension image = frame.displayImage.dimension();
        var transform = surfaceTransform(image, bounds, 1.0, focusRegion, new Point2D.Double());
        return normalizedToGraphics(transform, image, focusRegion.get());
    }

    private static TextInfo intertitleTextArea(Rectangle bounds) {
        Rectangle r = interTitleCenterRegion(bounds);
        int inset = 20;
        r.x += inset;
        r.y += inset;
        r.width -= 2 * inset;
        r.height -= 2 * inset;
        return new TextInfo(r, true);
    }

    private static TextInfo spokenTextArea(Rectangle bounds, Optional<Rectangle> focusArea) {
        float insetFactor = 0.05f;
        Insets insets = new Insets(
                (int) (bounds.height * insetFactor),
                (int) (bounds.width * insetFactor),
                (int) (bounds.height * insetFactor) + 100,
                (int) (bounds.width * insetFactor));

        int x;
        int y = insets.top;
        int textwidth = Math.max(FontLimits.MINIMAL_TEXT_WIDTH,
                (int) (bounds.getWidth() * Transform.goldenRatioFactorB));
        boolean enoughSpaceRight = bounds.width - focusArea.get().getMaxX() >= textwidth;
        boolean moreSpaceOnRight = bounds.width - focusArea.get().getMaxX() > focusArea.get().getMinX();
        var rightAlinged = focusArea.isPresent() ? enoughSpaceRight || moreSpaceOnRight : false;
        if (rightAlinged) {
            x = (int) focusArea.get().getMaxX() + insets.left * 2;
        } else {
            x = (int) focusArea.get().getMinX() - textwidth - insets.right * 2;
        }

        // TODO Limits do not always work because they are calculated before measuring the text
        // -> measure text first, then position the text and honor inset distance from focus region

        int leftLimit = bounds.x + insets.left;
        if (x < leftLimit) {
            int headroom = textwidth - FontLimits.MINIMAL_TEXT_WIDTH;
            if (headroom > 0) {
                int shift = Math.min(headroom, -x - leftLimit);
                x += shift;
                textwidth -= shift;
            }
            if (x < leftLimit) {
                x = leftLimit;
            }
        }

        int rightLimit = bounds.x + bounds.width - insets.right;
        if (x + textwidth > rightLimit) {
            int headroom = textwidth - FontLimits.MINIMAL_TEXT_WIDTH;
            if (headroom > 0) {
                int shift = Math.min(headroom, x - rightLimit);
                x -= shift;
                textwidth -= shift;
            }
            if (x > rightLimit - textwidth) {
                x = rightLimit - textwidth;
            }
        }

        // TODO if the text bubble covers the focus region, try to find a better matching region (e.g. below head ...)
        // TODO For large texts - e.g. showAll - it might make sense to align the bubble to the side

        int width = textwidth;
        int height = Math.max(FontLimits.MINIMAL_TEXT_HEIGHT, bounds.height - insets.bottom);
        return new TextInfo(new Rectangle(x, y, width, height), rightAlinged);
    }

    interface TextVisitor {
        static final TextVisitor MeasureText = (layout, x, y) -> { //
        };

        void render(TextLayout textLayout, float x, float y);
    }

    private static Rectangle renderText(Graphics2D g2d, String string, TextInfo textInfo, boolean intertitleActive) {
        if (string.isBlank()) {
            return textInfo.region;
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // When enabled short texts are wrapped on multiple lines although the text bubble has enough space
            // + longer texts are good - suggests that calculations are right
            // + longer texts have correct bottom inset
            // - for single word commands with trailing . the dot is renderer on the next line
            // - short text does not seem to have any bottom inset
            // - short text "Eins." is rendered in a single render call but the dot ends up on a new line.
            // g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            float dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            float rows = 8.0f;
            float cols = 6.0f * rows;
            float fontSize = (float) Math.min(textInfo.region.getWidth() / rows, textInfo.region.getHeight() / cols)
                    * dpi / 72.0f;

            FontRenderContext frc = g2d.getFontRenderContext();
            AttributedString text = new AttributedString(string);
            text.addAttribute(TextAttribute.JUSTIFICATION, TextAttribute.JUSTIFICATION_FULL);
            AttributedCharacterIterator paragraph;
            LineBreakMeasurer measurer;
            Rectangle layoutRegion;
            while (true) {
                Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize);
                text.addAttribute(TextAttribute.FONT, font);
                g2d.setFont(font);
                paragraph = text.getIterator();
                measurer = new LineBreakMeasurer(paragraph, frc);
                layoutRegion = renderText(textInfo.region, paragraph, measurer, TextVisitor.MeasureText);
                if (layoutRegion.getHeight() < textInfo.region.height || fontSize <= FontLimits.MINIMAL_FONT_SIZE) {
                    break;
                }
                fontSize /= 1.25f;
            }

            if (!textInfo.rightAligned) {
                layoutRegion.x += textInfo.region.width - layoutRegion.getWidth();
            }

            Rectangle adjustedTextArea;
            if (intertitleActive) {
                adjustedTextArea = textInfo.region;
                g2d.setBackground(TRANSPARENT);
                g2d.clearRect(textInfo.region.x, textInfo.region.y, textInfo.region.width, textInfo.region.height);
            } else {
                adjustedTextArea = renderTextBubble(g2d, layoutRegion.getBounds(), fontSize);
            }

            g2d.setColor(intertitleActive ? Color.white : Color.black);
            TextVisitor drawText = (layout, x, y) -> layout.draw(g2d, x, y);
            renderText(layoutRegion, paragraph, measurer, drawText);

            return adjustedTextArea;
        }
    }

    private static Rectangle renderTextBubble(Graphics2D g2d, Rectangle layoutRegion, float fontSize) {
        int arcWidth = (int) fontSize * 2;
        int inset = (int) (fontSize / 2.0f);
        var bubble = new Insets(1 * inset, 1 * inset, 1 * inset, 1 * inset);
        var r = new Rectangle(
                (int) (layoutRegion.getX() - bubble.left),
                (int) (layoutRegion.getY() - bubble.top),
                (int) layoutRegion.getWidth() + bubble.left + bubble.right,
                (int) layoutRegion.getHeight() + bubble.top + bubble.bottom);
        g2d.setBackground(TRANSPARENT);
        g2d.clearRect(r.x, r.y, r.width, r.height);
        g2d.setColor(new Color(224, 224, 224, 128));
        g2d.fillRoundRect(r.x, r.y, r.width, r.height, arcWidth, arcWidth);
        return r;
    }

    private static void renderIntertitle(Graphics2D g2d, Rectangle bounds, float alpha) {
        Rectangle centerRegion = interTitleCenterRegion(bounds);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f * alpha));
        g2d.fillRect(bounds.x, bounds.y, bounds.width, centerRegion.y);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.80f * alpha));
        g2d.fillRect(centerRegion.x, centerRegion.y, centerRegion.width, centerRegion.height);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f * alpha));
        g2d.fillRect(bounds.x, centerRegion.y + centerRegion.height, bounds.width,
                bounds.height - centerRegion.height - centerRegion.y - bounds.y);
    }

    private static Rectangle interTitleCenterRegion(Rectangle bounds) {
        Rectangle centerRegion = new Rectangle(0, bounds.height * 1 / 4, bounds.width, bounds.height * 2 / 4);
        return centerRegion;
    }

    private static Rectangle renderText(Rectangle textArea, AttributedCharacterIterator paragraph,
            LineBreakMeasurer measurer, TextVisitor render) {
        measurer.setPosition(paragraph.getBeginIndex());
        float wrappingWidth = (float) textArea.getWidth();
        float dy = textArea.y;
        Rectangle layoutRegion = null;
        while (measurer.getPosition() < paragraph.getEndIndex()) {
            paragraph.setIndex(measurer.getPosition());
            char ch;
            while ((ch = paragraph.next()) != CharacterIterator.DONE) {
                if (ch == '\n') {
                    break;
                }
            }
            int limit = paragraph.getIndex();
            TextLayout layout = null;
            if (limit > 0) {
                layout = measurer.nextLayout(wrappingWidth, limit, true);
                if (layout == null) {
                    layout = measurer.nextLayout(wrappingWidth, limit, false);
                }
            } else {
                layout = measurer.nextLayout(wrappingWidth);
            }

            float dx = layout.isLeftToRight() ? textArea.x : (wrappingWidth - layout.getAdvance());
            dy += (layout.getAscent());
            render.render(layout, dx, dy);
            dy += layout.getDescent() + layout.getLeading();
            if (layoutRegion == null) {
                layoutRegion = new Rectangle(
                        textArea.x, textArea.y,
                        (int) layout.getAdvance(),
                        (int) dy - textArea.y);
            } else {
                layoutRegion.width = Math.max((int) layoutRegion.getWidth(), (int) layout.getAdvance());
                layoutRegion.height = (int) dy - layoutRegion.y;
            }
        }
        return layoutRegion != null ? layoutRegion : textArea;
    }

}
