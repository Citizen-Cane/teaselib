package teaselib.hosts;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.Optional;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.ai.perception.HumanPose.Proximity;
import teaselib.core.ai.perception.ProximitySensor;

/**
 * @author Citizen-Cane
 *
 */
public class BufferedImageRenderer {

    private static final BufferedImageOp BLUR_OP = ConvolveEdgeReflectOp.blur(17);
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    final Image backgroundImage;

    public BufferedImageRenderer(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public void renderBackgound(RenderState frame, Rectangle bounds, Color backgroundColor) {
        if (frame.repaintSceneImage) {
            frame.sceneImage = newOrSameImage(frame.sceneImage, bounds);
            boolean backgroundVisible = frame.displayImage == null || frame.pose.distance.isEmpty();
            if (backgroundVisible) {
                var g2d = frame.sceneImage.createGraphics();
                g2d.setBackground(backgroundColor);
                g2d.clearRect(0, 0, bounds.width, bounds.height);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.drawImage(backgroundImage,
                        0, 0, bounds.width, bounds.height,
                        0, 0, backgroundImage.getWidth(null), backgroundImage.getHeight(null) * bounds.height / bounds.width, null);
                g2d.dispose();
            }
        }
    }

    void render(RenderState frame, Rectangle bounds) {
        BufferedImage sceneImage = renderScene(frame, bounds);
        BufferedImage textImage = renderText(frame, bounds);

        frame.renderedImage = newOrSameImage(frame.renderedImage, bounds);
        Graphics2D g2d = frame.renderedImage.createGraphics();
        if (frame.focusLevel < 1.0) {
            g2d.drawImage(sceneImage, BLUR_OP, 0, 0);
        } else {
            g2d.drawImage(sceneImage, 0, 0, null);
        }
        g2d.drawImage(textImage, 0, 0, null);
        g2d.dispose();
    }

    public BufferedImage renderScene(RenderState currentFrame, Rectangle bounds) {
        if (currentFrame.repaintSceneImage) {
            renderDisplayImage(currentFrame, currentFrame.sceneImage, bounds);
        }
        return currentFrame.sceneImage;
    }

    public static BufferedImage newBufferedImage(Rectangle bounds) {
        return newBufferedImage(bounds.width, bounds.height);
    }

    private static BufferedImage newBufferedImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private static Rectangle interTitleCenterRegion(Rectangle bounds) {
        Rectangle centerRegion = new Rectangle(0, bounds.height * 1 / 4, bounds.width, bounds.height * 2 / 4);
        return centerRegion;
    }

    private static void renderDisplayImage(RenderState frame, BufferedImage sceneImage, Rectangle bounds) {
        if (frame.repaintSceneImage) {
            var g2d = sceneImage.createGraphics();
            if (frame.displayImage != null) {
                BufferedImage displayImage = frame.displayImage;
                var displayImageSize = Transform.dimension(displayImage);
                var t = surfaceTransform(frame.actorZoom, displayImageSize, frame.pose, new Rectangle2D.Double(0.0, 0.0, bounds.width, bounds.height));
                g2d.drawImage(displayImage, t, null);
                renderDebugInfo(g2d, displayImageSize, frame.pose, t, bounds, frame.isIntertitle);
            } else {
                g2d.setBackground(TRANSPARENT);
                g2d.clearRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            g2d.dispose();
        }
    }

    private static AffineTransform surfaceTransform(double actorZoom, Dimension image,
            HumanPose.Estimation pose, Rectangle2D bounds) {
        AffineTransform surface;
        if (actorZoom > ProximitySensor.zoom.get(Proximity.FACE2FACE)) {
            // TODO make zoom depend on distance to focus solely on the region of interest
            surface = surfaceTransform(image, bounds, pose.boobs(), actorZoom);
        } else if (actorZoom > ProximitySensor.zoom.get(Proximity.AWAY)) {
            surface = surfaceTransform(image, bounds, pose.face(), actorZoom);
        } else {
            surface = surfaceTransform(image, bounds, Optional.empty(), actorZoom);
        }
        surface.preConcatenate(AffineTransform.getTranslateInstance(bounds.getMinX(), bounds.getMinY()));
        return surface;
    }

    private static AffineTransform surfaceTransform(Dimension image, Rectangle2D bounds,
            Optional<Rectangle2D> focusArea, double zoom) {
        var surface = Transform.maxImage(image, bounds, focusArea);
        if (focusArea.isPresent()) {
            Rectangle2D imageFocusArea = Transform.scale(focusArea.get(), image);
            surface = Transform.matchGoldenRatioOrKeepVisible(surface, image, bounds, imageFocusArea);
            if (zoom > 1.0) {
                surface = Transform.zoom(surface, imageFocusArea, zoom);
            }
        }
        return surface;
    }

    static void renderDebugInfo(Graphics2D g2d, Dimension image, HumanPose.Estimation pose, AffineTransform surface,
            Rectangle bounds, boolean intertitleActive) {
        drawBackgroundImageIconVisibleBounds(g2d, bounds);
        drawImageBounds(g2d, image, surface);
        if (pose != HumanPose.Estimation.NONE) {
            drawPosture(g2d, image, pose, surface);
        }
        if (!intertitleActive) {
            fillTextArea(g2d, bounds, spokenTextArea(bounds).x);
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
        g2d.drawRect((int) p0.getX(), (int) p0.getY(), (int) (p1.getX() - p0.getX()) - 1,
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
            g2d.setColor(face.isPresent() ? Color.cyan : Color.orange);
            g2d.drawOval((int) p.getX() - 2, (int) p.getY() - 2, 2 * 2, 2 * 2);
            g2d.setColor(face.isPresent() ? Color.cyan.darker().darker() : Color.red.brighter().brighter());
            g2d.drawOval((int) p.getX() - radius, (int) p.getY() - radius, 2 * radius, 2 * radius);
        }

        pose.face().ifPresent(region -> drawRegion(g2d, image, surface, region));
        pose.boobs().ifPresent(region -> drawRegion(g2d, image, surface, region));
    }

    private static void drawRegion(Graphics2D g2d, Dimension image, AffineTransform surface, Rectangle2D region) {
        var scale = AffineTransform.getScaleInstance(image.getWidth(), image.getHeight());
        var rect = scale.createTransformedShape(region);
        var r = surface.createTransformedShape(rect).getBounds2D();
        g2d.setColor(Color.blue);
        g2d.drawRect((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
    }

    private static void fillTextArea(Graphics2D g2d, Rectangle bounds, int textAreaInsetRight) {
        g2d.setColor(new Color(128, 128, 128, 64));
        g2d.fillRect(textAreaInsetRight, 0, bounds.width, bounds.height);
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

    private static final float FONT_SIZE = 36;
    private static final float MINIMAL_FONT_SIZE = 6;
    private static final float PARAGRAPH_SPACING = 1.5f;
    private static final int TEXT_AREA_BORDER = 10;

    private static final int TEXT_AREA_INSET = 40;
    private static final int TEXT_AREA_MAX_WIDTH = 12 * TEXT_AREA_INSET;
    private static final int TEXT_AREA_MIN_WIDTH = 6 * TEXT_AREA_INSET;

    private static BufferedImage renderText(RenderState frame, Rectangle bounds) {
        if (frame.repaintTextImage) {
            return renderText(frame, bounds, frame.text, frame.isIntertitle);
        } else {
            return frame.textImage;
        }
    }

    private static BufferedImage renderText(RenderState frame, Rectangle bounds, String text, boolean intertitleActive) {
        var textArea = intertitleActive ? intertitleTextArea(bounds) : spokenTextArea(bounds);
        if (frame.textImage == null) {
            frame.textImage = newBufferedImage(bounds);
        } else if (bounds.width != frame.textImage.getWidth() || bounds.height != frame.textImage.getHeight()) {
            frame.textImage = newBufferedImage(bounds);
        }
        frame.textImage = newOrSameImage(frame.textImage, bounds);
        if (!text.isBlank() || intertitleActive) {
            Graphics2D g2d = frame.textImage.createGraphics();
            g2d.setBackground(TRANSPARENT);
            g2d.clearRect(bounds.x, bounds.y, bounds.width, bounds.height);
            renderText(g2d, text, bounds, textArea, intertitleActive);
            g2d.dispose();
        }
        return frame.textImage;
    }

    private static BufferedImage newOrSameImage(BufferedImage image, Rectangle bounds) {
        if (image == null) {
            return newBufferedImage(bounds);
        } else if (bounds.width != image.getWidth() || bounds.height != image.getHeight()) {
            return newBufferedImage(bounds);
        } else {
            return image;
        }
    }

    private static Rectangle intertitleTextArea(Rectangle bounds) {
        Rectangle r = interTitleCenterRegion(bounds);
        int inset = 20;
        r.x += inset;
        r.y += inset;
        r.width -= 2 * inset;
        r.height -= 2 * inset;
        return r;
    }

    private static Rectangle spokenTextArea(Rectangle2D bounds) {
        int textAreaWidth = (int) Math.min(TEXT_AREA_MAX_WIDTH, bounds.getWidth() / 3.0);
        if (textAreaWidth < TEXT_AREA_MIN_WIDTH) {
            textAreaWidth = Math.min(TEXT_AREA_MIN_WIDTH, (int) (bounds.getWidth() * 0.8f));
        }

        int scaledInset = TEXT_AREA_INSET * textAreaWidth / TEXT_AREA_MAX_WIDTH;
        if (bounds.getWidth() < textAreaWidth + 2 * scaledInset) {
            textAreaWidth = (int) (bounds.getWidth() * 0.9);
            scaledInset = (int) (bounds.getWidth() * 0.05);
        }

        int topInset = 2 * scaledInset;
        int bottomInset = 4 * scaledInset;

        Rectangle textArea = new Rectangle((int) bounds.getX() + (int) bounds.getWidth() - textAreaWidth - scaledInset, //
                topInset, //
                textAreaWidth, //
                (int) bounds.getHeight() - bottomInset - topInset);
        return textArea;
    }

    interface TextVisitor {
        void render(TextLayout textLayout, float x, float y);
    }

    private static void renderText(Graphics2D g2d, String string, Rectangle bounds, Rectangle textArea,
            boolean intertitleActive) {
        if (intertitleActive) {
            renderIntertitle(g2d, bounds);
        }

        if (!string.isBlank()) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontRenderContext frc = g2d.getFontRenderContext();
            AttributedCharacterIterator paragraph;
            LineBreakMeasurer measurer;
            Dimension2D textSize = new Dimension(0, 0);
            TextVisitor measureText = (TextLayout layout, float x, float y) -> textSize
                    .setSize(Math.max(textSize.getWidth(), layout.getAdvance()), y + layout.getDescent() - textArea.y);

            float dpi = Toolkit.getDefaultToolkit().getScreenResolution();
            float fontSize = FONT_SIZE * dpi / 72.0f;

            while (true) {
                Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize);
                g2d.setFont(font);
                AttributedString text = new AttributedString(string);
                text.addAttribute(TextAttribute.FONT, font);
                paragraph = text.getIterator();
                measurer = new LineBreakMeasurer(paragraph, frc);
                renderText(textArea, paragraph, measurer, measureText);
                if (textSize.getHeight() < textArea.height || fontSize <= MINIMAL_FONT_SIZE) {
                    break;
                }
                fontSize /= 1.25f;
            }

            if (!intertitleActive) {
                var adjustedTextArea = new Rectangle(textArea.x - TEXT_AREA_BORDER, textArea.y - TEXT_AREA_BORDER,
                        (int) textSize.getWidth() + 2 * TEXT_AREA_BORDER,
                        (int) textSize.getHeight() + 2 * TEXT_AREA_BORDER);
                renderTextBubble(g2d, adjustedTextArea);
            }

            TextVisitor drawText = (TextLayout layout, float x, float y) -> layout.draw(g2d, x, y);
            renderText(textArea, paragraph, measurer, drawText);
        }
    }

    private static void renderTextBubble(Graphics2D g2d, Rectangle textArea) {
        int arcWidth = 40;
        g2d.setColor(new Color(224, 224, 224, 160));
        g2d.fillRoundRect(textArea.x, textArea.y, textArea.width, textArea.height, arcWidth, arcWidth);
        g2d.setColor(Color.black);
    }

    private static void renderIntertitle(Graphics2D g2d, Rectangle bounds) {
        Rectangle centerRegion = interTitleCenterRegion(bounds);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
        g2d.fillRect(bounds.x, bounds.y, bounds.width, centerRegion.y);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.80f));
        g2d.fillRect(centerRegion.x, centerRegion.y, centerRegion.width, centerRegion.height);
        g2d.setColor(new Color(0.0f, 0.0f, 0.0f, 0.65f));
        g2d.fillRect(bounds.x, centerRegion.y + centerRegion.height, bounds.width,
                bounds.height - centerRegion.height - centerRegion.y - bounds.y);
        g2d.setColor(Color.white);
    }

    private static void renderText(Rectangle textArea, AttributedCharacterIterator paragraph,
            LineBreakMeasurer measurer, TextVisitor render) {
        measurer.setPosition(paragraph.getBeginIndex());
        float wrappingWidth = (float) textArea.getWidth();
        float dy = textArea.y;
        while (measurer.getPosition() < paragraph.getEndIndex()) {
            paragraph.setIndex(measurer.getPosition());
            char ch;
            while ((ch = paragraph.next()) != CharacterIterator.DONE) {
                if (ch == '\n') {
                    break;
                }
            }
            TextLayout layout;
            int limit = paragraph.getIndex();
            if (limit > 0) {
                layout = measurer.nextLayout(wrappingWidth, limit, true);
                if (layout == null) {
                    layout = measurer.nextLayout(wrappingWidth, limit, false);
                }
            } else {
                layout = measurer.nextLayout(wrappingWidth);
            }

            dy += (layout.getAscent());
            float dx = layout.isLeftToRight() ? textArea.x : (wrappingWidth - layout.getAdvance());

            render.render(layout, dx, dy);
            dy += layout.getDescent() + layout.getLeading();

            if (measurer.getPosition() == limit) {
                dy += layout.getAscent() * (PARAGRAPH_SPACING - 1.0f);
            }
        }
    }

}
