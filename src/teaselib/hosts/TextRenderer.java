package teaselib.hosts;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.Optional;

/**
 * @author Citizen-Cane
 *
 */
class TextRenderer {

    /**
     * The minimum limits may cause text overflow but at small window size this looks more reasonable than incredibly
     * small font size.
     */
    class FontLimits {
        static final float MINIMAL_FONT_SIZE = 6.0f;
        static final int MINIMAL_TEXT_WIDTH = 100;
        static final int MINIMAL_TEXT_HEIGHT = 100;
    }

    private record TextInfo(Rectangle region, boolean rightAligned) { //
    }

    void drawText(Graphics2D g2d, RenderState frame, Rectangle bounds, Optional<Rectangle> focusArea) {
        if (!frame.text.isBlank()) {
            var textInfo = frame.isIntertitle ? intertitleTextArea(bounds) : spokenTextArea(bounds, focusArea);
            layoutText(g2d, frame.text, textInfo);
            frame.textImageRegion = drawTextRegion(g2d, frame, textInfo);
            g2d.setColor(frame.isIntertitle ? Color.white : Color.black);
            renderText(g2d);
        }
    }

    private Rectangle drawTextRegion(Graphics2D g2d, RenderState frame, TextInfo textInfo) {
        Rectangle adjustedTextArea;
        if (frame.isIntertitle) {
            adjustedTextArea = textInfo.region;
            g2d.setBackground(SceneRenderer.TRANSPARENT);
            g2d.clearRect(textInfo.region.x, textInfo.region.y, textInfo.region.width, textInfo.region.height);
        } else {
            adjustedTextArea = drawTextBubble(g2d, region.getBounds(), fontSize);
        }
        return adjustedTextArea;
    }

    private static TextRenderer.TextInfo intertitleTextArea(Rectangle bounds) {
        Rectangle r = interTitleCenterRegion(bounds);
        int inset = 20;
        r.x += inset;
        r.y += inset;
        r.width -= 2 * inset;
        r.height -= 2 * inset;
        return new TextInfo(r, true);
    }

    static Rectangle interTitleCenterRegion(Rectangle bounds) {
        Rectangle centerRegion = new Rectangle(0, bounds.height * 1 / 4, bounds.width, bounds.height * 2 / 4);
        return centerRegion;
    }

    private static final Rectangle2D DefaultFocusRegion = new Rectangle2D.Double(0.25, 0.25, 0.25, 0.25);

    private static TextRenderer.TextInfo spokenTextArea(Rectangle bounds, Optional<Rectangle> focusArea) {
        return spokenTextArea(bounds, focusArea.isPresent()
                ? focusArea.get()
                : new Rectangle(
                        (int) (bounds.x + bounds.width * DefaultFocusRegion.getX()),
                        (int) (bounds.y + bounds.height * DefaultFocusRegion.getY()),
                        (int) (bounds.width * DefaultFocusRegion.getWidth()),
                        (int) (bounds.height * DefaultFocusRegion.getHeight())));
    }

    private static TextRenderer.TextInfo spokenTextArea(Rectangle bounds, Rectangle focusArea) {
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
        boolean enoughSpaceRight = bounds.width - focusArea.getMaxX() >= textwidth;
        boolean moreSpaceOnRight = bounds.width - focusArea.getMaxX() > focusArea.getMinX();
        var rightAlinged = enoughSpaceRight || moreSpaceOnRight;
        if (rightAlinged) {
            x = (int) focusArea.getMaxX() + insets.left * 2;
        } else {
            x = (int) focusArea.getMinX() - textwidth - insets.right * 2;
        }

        // the text area may become smaller when shifted into the bounds -> text may also become smaller
        // - very irritating when there is obviously enough space towards the borders
        // This is As Designed but has to be improved -> see subsequent TODOs

        // TODO find better areas to place text -> make text placing more natural
        // - currently text is always placed at the top

        // TODO Limits do not always work because they are calculated before measuring the text
        // -> measure text first, then position the text and honor inset distance from focus region

        int leftLimit = bounds.x + insets.left;
        if (x < leftLimit) {
            int headroom = textwidth - FontLimits.MINIMAL_TEXT_WIDTH;
            if (headroom > 0) {
                int shift = Math.min(headroom, leftLimit - x);
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
                int shift = Math.min(headroom, x + textwidth - rightLimit);
                x -= shift;
                textwidth -= shift;
            }
            if (x > rightLimit - textwidth) {
                x = rightLimit - textwidth;
            }
        }

        // TODO if the text bubble covers the focus region, try to find a better matching region (e.g. below head
        // ...)
        // TODO For large texts - e.g. showAll - it might make sense to align the bubble to the side

        int width = textwidth;
        int height = Math.max(FontLimits.MINIMAL_TEXT_HEIGHT, bounds.height - insets.bottom);
        return new TextInfo(new Rectangle(x, y, width, height), rightAlinged);
    }

    private static Rectangle drawTextBubble(Graphics2D g2d, Rectangle layoutRegion, float fontSize) {
        int arcWidth = (int) fontSize * 2;
        int inset = (int) (fontSize / 2.0f);
        var bubble = new Insets(1 * inset, 1 * inset, 1 * inset, 1 * inset);
        var r = new Rectangle(
                (int) (layoutRegion.getX() - bubble.left),
                (int) (layoutRegion.getY() - bubble.top),
                (int) layoutRegion.getWidth() + bubble.left + bubble.right,
                (int) layoutRegion.getHeight() + bubble.top + bubble.bottom);
        g2d.setBackground(SceneRenderer.TRANSPARENT);
        g2d.clearRect(r.x, r.y, r.width, r.height);
        g2d.setColor(new Color(224, 224, 224, 128));
        g2d.fillRoundRect(r.x, r.y, r.width, r.height, arcWidth, arcWidth);
        return r;
    }

    private interface TextVisitor {
        void run(TextLayout textLayout, float x, float y);
    }

    private static class Measurer implements TextVisitor {
        private final Rectangle textArea;
        Rectangle region;

        public Measurer(Rectangle textArea) {
            this.textArea = textArea;
        }

        @Override
        public void run(TextLayout textLayout, float x, float y) {
            if (region == null) {
                region = new Rectangle(
                        textArea.x, textArea.y,
                        (int) textLayout.getAdvance(),
                        (int) y - textArea.y);
            } else {
                region.width = Math.max((int) region.getWidth(), (int) textLayout.getAdvance());
                region.height = (int) y - region.y;
            }
        }

    }

    float fontSize;
    AttributedCharacterIterator paragraph;
    LineBreakMeasurer measurer;
    Rectangle region;

    private void layoutText(Graphics2D g2d, String string, TextRenderer.TextInfo textInfo) {
        float cols = 24.0f;
        float rows = 12.0f;
        float dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        fontSize = (float) Math.min(textInfo.region.getWidth() / cols, textInfo.region.getHeight() / rows) * dpi / 72.0f;
        FontRenderContext frc = g2d.getFontRenderContext();
        AttributedString text = new AttributedString(string);
        text.addAttribute(TextAttribute.JUSTIFICATION, TextAttribute.JUSTIFICATION_FULL);
        while (true) {
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize);
            text.addAttribute(TextAttribute.FONT, font);
            g2d.setFont(font);
            paragraph = text.getIterator();
            measurer = new LineBreakMeasurer(paragraph, frc);
            Measurer textAreaNeasurer = new Measurer(textInfo.region);
            layoutText(textInfo.region, paragraph, measurer, textAreaNeasurer);
            region = textAreaNeasurer.region;
            if (region.getHeight() < textInfo.region.height || fontSize <= FontLimits.MINIMAL_FONT_SIZE) {
                break;
            }
            fontSize /= 1.25f;
        }
        if (!textInfo.rightAligned) {
            region.x += textInfo.region.width - region.getWidth();
        }
    }

    private void renderText(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // When enabled short texts are wrapped on multiple lines although the text bubble has enough space
        // + longer texts are good - suggests that calculations are right
        // + longer texts have correct bottom inset
        // - for single word commands with trailing . the dot is renderer on the next line
        // - short text does not seem to have any bottom inset
        // - short text "Eins." is rendered in a single render call but the dot ends up on a new line.
        // g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
        // RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        layoutText(region, paragraph, measurer, (layout, x, y) -> layout.draw(g2d, x, y));
    }

    private static void layoutText(Rectangle textArea, AttributedCharacterIterator paragraph,
            LineBreakMeasurer measurer, TextRenderer.TextVisitor textVisitor) {
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
            textVisitor.run(layout, dx, dy);
            dy += layout.getDescent() + layout.getLeading();
        }
    }

}
