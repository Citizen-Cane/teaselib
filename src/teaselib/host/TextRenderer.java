package teaselib.host;

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

    private record LayoutInfo(Rectangle region, Insets insets, boolean rightAligned, boolean isIntertitle) { //
    }

    LayoutInfo layoutInfo;
    float fontSize;
    AttributedCharacterIterator paragraph;
    LineBreakMeasurer measurer;

    void drawText(Graphics2D g2d, RenderState frame, Rectangle bounds, Optional<Rectangle> focusArea) {
        if (!frame.text.isBlank()) {
            layoutInfo = frame.isIntertitle ? intertitleTextArea(bounds) : spokenTextArea(bounds, focusArea);
            var layoutRegion = layoutText(g2d, frame.text, bounds);
            alignLayout(layoutRegion, bounds, focusArea);
            frame.textImageRegion = drawTextRegion(g2d, frame, layoutRegion);
            g2d.setColor(frame.isIntertitle ? Color.white : Color.black);
            drawText(g2d, layoutRegion);
        }
    }

    private static TextRenderer.LayoutInfo intertitleTextArea(Rectangle bounds) {
        Rectangle r = interTitleCenterRegion(bounds);
        int inset = 20;
        Insets insets = new Insets(inset, inset, inset, inset);
        r.x += insets.left;
        r.y += insets.top;
        r.width -= insets.left + insets.right;
        r.height -= insets.top + insets.bottom;
        return new LayoutInfo(r, insets, true, true);
    }

    static Rectangle interTitleCenterRegion(Rectangle bounds) {
        Rectangle centerRegion = new Rectangle(0, bounds.height * 1 / 4, bounds.width, bounds.height * 2 / 4);
        return centerRegion;
    }

    private static final Rectangle2D DefaultFocusRegion = new Rectangle2D.Double(0.25, 0.25, 0.25, 0.25);

    /**
     * The minimum limits may cause text overflow but at small window size this looks more reasonable than incredibly
     * small font size.
     */
    static class FontLimits {
        static final float MINIMAL_FONT_SIZE = 6.0f;
        static final int MINIMAL_TEXT_WIDTH = 100;
        static final int MINIMAL_TEXT_HEIGHT = 100;
    }

    private static TextRenderer.LayoutInfo spokenTextArea(Rectangle bounds, Optional<Rectangle> focusArea) {
        return spokenTextArea(bounds, focusArea.isPresent()
                ? focusArea.get()
                : new Rectangle(
                        (int) (bounds.x + bounds.width * DefaultFocusRegion.getX()),
                        (int) (bounds.y + bounds.height * DefaultFocusRegion.getY()),
                        (int) (bounds.width * DefaultFocusRegion.getWidth()),
                        (int) (bounds.height * DefaultFocusRegion.getHeight())));
    }

    private static TextRenderer.LayoutInfo spokenTextArea(Rectangle bounds, Rectangle focusArea) {
        float insetFactor = 0.05f;
        Insets insets = new Insets(
                (int) (bounds.height * insetFactor),
                (int) (bounds.width * insetFactor),
                (int) (bounds.height * insetFactor) + 100,
                (int) (bounds.width * insetFactor));

        int x;
        int y = insets.top;
        int textwidth = textWidth(bounds, Transform.goldenRatioFactorA);
        int minimalTextWidth = textWidth(bounds, Transform.goldenRatioFactorB);
        boolean enoughSpaceRight = bounds.width - focusArea.getMaxX() >= textwidth;
        boolean moreSpaceOnRight = bounds.width - focusArea.getMaxX() > focusArea.getMinX();
        var rightAlinged = enoughSpaceRight || moreSpaceOnRight;
        if (rightAlinged) {
            x = (int) focusArea.getMaxX() + insets.left * 2;
        } else {
            x = (int) focusArea.getMinX() - textwidth - insets.right * 2;
        }

        // Shrink left up to minimal text width
        int leftLimit = bounds.x + insets.left;
        if (x < leftLimit) {
            int headroom = textwidth - minimalTextWidth;
            if (headroom > 0) {
                int shift = Math.min(headroom, leftLimit - x);
                x += shift;
                textwidth -= shift; // Don't make smaller, text box might become too small
            }
            if (x < leftLimit) {
                x = leftLimit;
            }
        }

        // Shrink right up to minimal text width
        int rightLimit = bounds.x + bounds.width - insets.right;
        if (x + textwidth > rightLimit) {
            int headroom = textwidth - minimalTextWidth;
            if (headroom > 0) {
                int shift = Math.min(headroom, x + textwidth - rightLimit);
                textwidth -= shift;
            }
            if (x + textwidth > rightLimit - textwidth) {
                x = rightLimit - textwidth;
            }
        }

        int width = textwidth;
        int height = Math.max(FontLimits.MINIMAL_TEXT_HEIGHT, bounds.height - insets.bottom);
        return new LayoutInfo(new Rectangle(x, y, width, height), insets, rightAlinged, false);
    }

    private static int textWidth(Rectangle bounds, double factor) {
        return Math.max(FontLimits.MINIMAL_TEXT_WIDTH, (int) (bounds.getWidth() * factor));
    }

    private void alignLayout(Rectangle layoutRegion, Rectangle bounds, Optional<Rectangle> focusArea) {
        if (layoutInfo.rightAligned) {
            if (layoutRegion.width < layoutInfo.region.width) {
                // adjust left/right alignment of text layout when line breaking resulted in smaller text area
                if (focusArea.isPresent()) {
                    double focusAreaRight = focusArea.get().getMaxX();
                    if (layoutRegion.x < focusAreaRight) {
                        layoutRegion.x += Math.min(
                                focusAreaRight - layoutRegion.x + layoutInfo.insets.left,
                                (bounds.x + bounds.width) - (layoutRegion.x + layoutRegion.width));
                    }
                }
            }
        } else { // Align spare space to the left
            if (layoutInfo.region.width - layoutRegion.getWidth() > 0) {
                layoutRegion.x += layoutInfo.region.width - layoutRegion.getWidth();
            }
        }
    }

    private Rectangle drawTextRegion(Graphics2D g2d, RenderState frame, Rectangle region) {
        Rectangle adjustedTextArea;
        if (frame.isIntertitle) {
            adjustedTextArea = region;
            g2d.setBackground(SceneRenderer.TRANSPARENT);
            g2d.clearRect(region.x, region.y, region.width, region.height);
        } else {
            adjustedTextArea = drawTextBubble(g2d, region.getBounds(), fontSize);
        }
        return adjustedTextArea;
    }

    private static Rectangle drawTextBubble(Graphics2D g2d, Rectangle layoutRegion, float fontSize) {
        int arcRadius = (int) fontSize * 2;
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
        g2d.fillRoundRect(r.x, r.y, r.width, r.height, arcRadius, arcRadius);
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
                        (int) (y - textArea.y + textLayout.getDescent()));
            } else {
                region.width = Math.max((int) region.getWidth(), (int) textLayout.getAdvance());
                region.height = (int) (y - region.y + textLayout.getDescent());
            }
        }

    }

    private Rectangle layoutText(Graphics2D g2d, String string, Rectangle bounds) {
        float cols = 48.0f;
        float rows = layoutInfo.isIntertitle ? 24.0f : 48.0f;
        float dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        float fontSizeByCols = bounds.width / cols;
        float fontSizeByRows = (float) layoutInfo.region.getHeight() / rows;
        fontSize = (float) Math.min(fontSizeByCols, fontSizeByRows) * dpi / 72.0f;
        FontRenderContext frc = g2d.getFontRenderContext();
        AttributedString text = new AttributedString(string);
        text.addAttribute(TextAttribute.JUSTIFICATION, TextAttribute.JUSTIFICATION_FULL);
        Rectangle region;
        while (true) {
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize);
            text.addAttribute(TextAttribute.FONT, font);
            g2d.setFont(font);
            paragraph = text.getIterator();
            measurer = new LineBreakMeasurer(paragraph, frc);
            Measurer textAreaNeasurer = new Measurer(layoutInfo.region);
            layoutText(layoutInfo.region, paragraph, measurer, textAreaNeasurer);
            region = textAreaNeasurer.region;
            if (region.getHeight() < layoutInfo.region.height || fontSize <= FontLimits.MINIMAL_FONT_SIZE) {
                break;
            }
            fontSize /= 1.25f;
        }
        return region;
    }

    private void drawText(Graphics2D g2d, Rectangle region) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        paragraph.setIndex(paragraph.getBeginIndex());
        layoutText(region, paragraph, measurer, (layout, x, y) -> layout.draw(g2d, x, y));
    }

    private static void layoutText(Rectangle textArea, AttributedCharacterIterator paragraph,
            LineBreakMeasurer measurer, TextRenderer.TextVisitor textVisitor) {
        measurer.setPosition(paragraph.getBeginIndex());
        float wrappingWidth = (float) textArea.getWidth();
        float dy = textArea.y;
        while (measurer.getPosition() < paragraph.getEndIndex()) {
            int limit = advanceLimitToNextNewLine(measurer, paragraph);
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
            textVisitor.run(layout, dx, dy - layout.getLeading());
            dy += layout.getDescent() + layout.getLeading();

            boolean moreTextFollows = measurer.getPosition() < paragraph.getEndIndex();
            if (moreTextFollows) {
                boolean endOfParagraph = measurer.getPosition() == limit;
                if (endOfParagraph) {
                    dy += layout.getAscent() * 0.5f;
                }
            }
        }
    }

    private static int advanceLimitToNextNewLine(LineBreakMeasurer measurer,
            AttributedCharacterIterator paragraph) {
        paragraph.setIndex(measurer.getPosition());
        char ch;
        while ((ch = paragraph.next()) != CharacterIterator.DONE) {
            if (ch == '\n') {
                break;
            }
        }
        int limit = paragraph.getIndex();
        return limit;
    }

    void drawDebugInfo(Graphics2D g2d) {
        g2d.setColor(Color.magenta);
        var r = layoutInfo.region;
        g2d.drawRect(r.x, r.y, r.width, r.height);
        g2d.drawLine(r.x, r.y, r.x + r.width, r.y + r.height);
        g2d.drawLine(r.x, r.y + r.height, r.x + r.width, r.y);
    }

}
