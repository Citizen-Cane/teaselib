package teaselib.host;

import static java.awt.Transparency.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.ai.perception.HumanPose;
import teaselib.util.AnnotatedImage;

/**
 * @author Citizen-Cane
 *
 */
public class Scene {

    static final Logger logger = LoggerFactory.getLogger(Scene.class);

    public final SceneRenderer renderer;

    RenderState previousImage = new RenderState();
    RenderState currentFrame = new RenderState();
    Rectangle transistionBounds;
    RenderState nextFrame = new RenderState();

    public Scene(SceneRenderer renderer) {
        this.renderer = renderer;
    }

    public void showInterTitle(List<String> text, Rectangle bounds) {
        synchronized (nextFrame) {
            nextFrame.isIntertitle = true;
            rotateTextOverlayBuffer(text, bounds);
            rememberPreviousImage();
        }
    }

    public void show(AnnotatedImage displayImage, List<String> text, GraphicsConfiguration gc, Rectangle bounds) {
        AbstractValidatedImage<?> image;
        HumanPose.Estimation pose;
        Set<AnnotatedImage.Annotation> annotations;
        boolean updateDisplayImage;
        if (displayImage != null) {
            updateDisplayImage = !displayImage.resource.equals(nextFrame.displayImageResource);
            try {
                // TODO only necessary when different from frame image but need to synchronize to test
                // -> cache in AnnotatedImage but on the other hand the images is supposed to be different on each call
                // + caching is good for random image sets where images of each take are displayed multiple times
                // -> cache images here to avoid using java.awt.Image outside host impl.
                if (updateDisplayImage) {
                    image = createDisplayImage(gc, displayImage);

                    pose = displayImage.pose;
                    annotations = displayImage.annotations;
                } else {
                    image = null;
                    pose = null;
                    annotations = null;
                }
            } catch (IOException e) {
                image = null;
                pose = HumanPose.Estimation.NONE;
                annotations = null;
                logger.error(e.getMessage(), e);
            }
        } else {
            updateDisplayImage = true;
            image = null;
            pose = HumanPose.Estimation.NONE;
            annotations = null;
        }

        synchronized (nextFrame) {
            if (updateDisplayImage) {
                if (displayImage != null) {
                    nextFrame.displayImageResource = displayImage.resource;
                    nextFrame.displayImage = image;
                    nextFrame.pose = pose;
                    nextFrame.annotations = annotations;
                } else if (nextFrame.displayImageResource != null) {
                    nextFrame.displayImageResource = null;
                    nextFrame.displayImage = image;
                    nextFrame.pose = pose;
                    nextFrame.annotations = annotations;
                }
            }
            nextFrame.isIntertitle = false;
            rotateTextOverlayBuffer(text, bounds);
            rememberPreviousImage();
            transistionBounds = bounds;
        }
    }

    public void setFocusLevel(float focusLevel) {
        synchronized (nextFrame) {
            nextFrame.focusLevel = focusLevel;
        }
    }

    public void setActorZoom(double zoom) {
        synchronized (nextFrame) {
            nextFrame.actorZoom = zoom;
        }
    }

    /**
     * Start point for blending images while moving from one focus region to the next. The start point for the
     * transition will be the focus region center point of the current actor image, assuming that both images feature
     * the same focus region type (for instance the face).
     * 
     * @param newFocus
     * @param currentFocus
     *
     * @throws NullPointerException
     *             When either current or new image is null
     * 
     * @return The start position of the new actor image.
     */
    public Point2D getTransitionVector(Point2D currentFocus, Point2D newFocus) {
        return renderer.getTransitionVector(previousImage, currentFocus, nextFrame, newFocus, transistionBounds);
    }

    public void setTransition(Point2D prev, double prevZoom, Point2D cur, double nextZoom, float sceneBlend,
            float textBlendIn, float textBlendOut) {
        synchronized (nextFrame) {
            // TODO next and previousImage render state logic belongs to scene renderer -> refactor out
            previousImage.displayImageOffset = new Point2D.Double(prev.getX(), prev.getY());
            previousImage.actorZoom = prevZoom;
            nextFrame.displayImageOffset = new Point2D.Double(cur.getX(), cur.getY());
            nextFrame.actorZoom = nextZoom;
            nextFrame.sceneBlend = sceneBlend;
            previousImage.sceneBlend = 1.0f - sceneBlend;

            previousImage.textBlend = textBlendOut;
            nextFrame.textBlend = textBlendIn;
        }
    }

    public void endScene(Rectangle bounds) {
        synchronized (nextFrame) {
            // Keep the image, remove any text to provide some feedback
            rotateTextOverlayBuffer(Collections.singletonList(""), bounds);
            rememberPreviousImage();
        }
    }

    public void resize() {
        currentFrame.repaintSceneImage = true;
        currentFrame.repaintTextImage = true;
    }

    private static AbstractValidatedImage<?> createDisplayImage(GraphicsConfiguration gc, AnnotatedImage displayImage)
            throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(displayImage.bytes));
        if (image.getColorModel().equals(gc.getColorModel())) {
            return new ValidatedBufferedImage(image);
        } else {
            BufferedImage compatible = gc.createCompatibleImage(image.getWidth(), image.getHeight(), OPAQUE);
            var g2d = compatible.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            return new ValidatedBufferedImage(compatible);
        }
    }

    private void rotateTextOverlayBuffer(List<String> text, Rectangle bounds) {
        nextFrame.text = text.stream().collect(Collectors.joining("\n"));
        nextFrame.textImage = new ValidatedBufferedImage(
                (gc, w, h, t) -> renderer.textOverlays.rotateBuffer(gc, bounds), Transparency.TRANSLUCENT);
        nextFrame.textImage.setSize(bounds.width, bounds.height);
    }

    private void rememberPreviousImage() {
        previousImage = currentFrame;
    }

    public boolean render(Graphics2D g2d, Rectangle bounds, GraphicsConfiguration gc, Color background) {
        synchronized (nextFrame) {
            nextFrame.updateFrom(currentFrame);
            currentFrame = nextFrame;
            renderer.render(g2d, gc, currentFrame, previousImage, bounds, background);
            nextFrame = currentFrame.copy();
            return !contentsLost(currentFrame) && !contentsLost(previousImage);
        }
    }

    private static boolean contentsLost(RenderState frame) {
        return contentsLost(frame.displayImage) || contentsLost(frame.textImage);
    }

    private static boolean contentsLost(AbstractValidatedImage<?> displayImage) {
        return displayImage != null && displayImage.contentsLost();
    }

}