package teaselib.core.ui;

import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Audio;
import teaselib.core.Host;
import teaselib.core.Persistence;
import teaselib.core.ResourceLoader;
import teaselib.core.configuration.Configuration;
import teaselib.util.AnnotatedImage;

/**
 * @author Citizen-Cane
 *
 */
public class AnimatedHost implements Host, Closeable {

    static final Logger logger = LoggerFactory.getLogger(AnimatedHost.class);

    private static final int ZOOM_DURATION = 200;
    private static final int TRANSITION_DURATION = 700;
    private final static long FRAMETIME_MILLIS = 16;

    final Host host;
    private final Thread animator;

    private AnnotatedImage currentImage = AnnotatedImage.NoImage;

    private double actualZoom = 1.0;
    private double expectedZoom = 1.0;

    private Point2D actualOffset = new Point2D.Double();
    private Point2D expectedOffset = new Point2D.Double();

    private float actualAlpha = 1.0f;
    private float expectedAlpha = 1.0f;

    private AnimationPath pathx = AnimationPath.NONE;
    private AnimationPath pathy = AnimationPath.NONE;
    private AnimationPath pathz = AnimationPath.NONE;
    private AnimationPath alpha = AnimationPath.NONE;

    public AnimatedHost(Host host) {
        this.host = host;
        this.animator = new Thread(this::animate, "Animate UI");
        this.animator.start();
    }

    @Override
    public void close() throws IOException {
        animator.interrupt();
        try {
            animator.join();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
        if (host instanceof Closeable closeable) {
            closeable.close();
        }
    }

    private void animate() {
        synchronized (animator) {
            while (!Thread.interrupted()) {
                try {
                    animator.wait();
                    animator.wait(FRAMETIME_MILLIS);
                    while (animationsRunning()) {
                        long now = System.currentTimeMillis();
                        actualOffset.setLocation(pathx.get(now), pathy.get(now));
                        actualZoom = pathz.get(now);
                        actualAlpha = (float) alpha.get(now);
                        host.setActorOffset(actualOffset);
                        host.setActorZoom(actualZoom);
                        host.setActorAlpha(actualAlpha);
                        host.show();
                        long finish = now;
                        long duration = FRAMETIME_MILLIS - (finish - now);
                        if (duration > 0) {
                            // TODO Sleep to be able to wait until finish
                            animator.wait(duration);
                        } else {
                            animator.wait(0);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private boolean animationsRunning() {
        return expectedZoom != actualZoom ||
                expectedAlpha != actualAlpha ||
                expectedOffset.getX() != actualOffset.getX() ||
                expectedOffset.getY() != actualOffset.getY();
    }

    @Override
    public Persistence persistence(Configuration configuration) throws IOException {
        return host.persistence(configuration);
    }

    @Override
    public Audio audio(ResourceLoader resources, String path) {
        return host.audio(resources, path);
    }

    @Override
    public void show(AnnotatedImage newImage, List<String> text) {
        synchronized (animator) {
            waitAnimationCompleted();

            float currentDistance = currentImage != null ? currentImage.pose.distance.orElse(0.0f) : 0.0f;
            float newDistance = newImage != null ? newImage.pose.distance.orElse(0.0f) : 0.0f;

            if (newDistance != 0.0f && newDistance < currentDistance) {
                // New image nearer
                if (actualZoom > 1.0) {
                    // current image zoomed
                    skipUnzoomAfterPrompt(newImage);
                } else {
                    // zoom-in existing image first for smooth focus region image change

                    // For wide translations results in the original image moved over the original image
                    // - looks stupid
                    // zoom actor farer away since this makes distance transitions more realistic

                    // TODO only if no or small transition
                    // zoomBeforeDisplayingNewImage(newImage, text, currentDistance, newDistance);

                    // expectedZoom = 1.0;
                    translateToNearerDistance(newImage);
                    // TODO sofa test images: slide-in from tight does not work
                }
            } else if (currentDistance != 0.0f && newDistance > currentDistance) {
                // new image farer
                translateToFarerDistance(newImage, currentDistance, newDistance);
            } else {
                translateToOrigin();
            }

            actualAlpha = 0.0f;
            currentImage = newImage;
            startAnimation(currentImage, text);
        }
    }

    private void translateToFarerDistance(AnnotatedImage newImage, float currentDistance, float newDistance) {
        actualZoom = newDistance / currentDistance;
        translateTo(newImage);
    }

    private void zoomBeforeDisplayingNewImage(AnnotatedImage newImage, List<String> text, float currentDistance, float newDistance) {
        // TODO any focus region
        Point2D newFocus = newImage.pose.head.get();
        Point2D currentFocus = currentImage.pose.head.get();
        double x = newFocus.getX() - currentFocus.getX();
        double y = newFocus.getY() - currentFocus.getY();
        expectedOffset.setLocation(x, y);
        expectedZoom = currentDistance / newDistance;

        startAnimation(currentImage, text);
        animator.notifyAll();
        waitAnimationCompleted();

        actualOffset = new Point2D.Double(0.0, 0.0);
        expectedOffset = new Point2D.Double(0.0, 0.0);
        actualZoom = 1.0;
        expectedZoom = 1.0;
    }

    private void translateToNearerDistance(AnnotatedImage newImage) {
        translateTo(newImage);
    }

    private void skipUnzoomAfterPrompt(AnnotatedImage newImage) {
        actualZoom = 1.0;
        expectedZoom = 1.0;
        translateTo(newImage);
    }

    private void translateToOrigin() {
        expectedOffset = new Point2D.Double(0.0, 0.0);
        // expectedZoom = 1.0;
    }

    private void translateTo(AnnotatedImage newImage) {
        // TODO any focus region
        Point2D newFocus = newImage.pose.head.get();
        Point2D currentFocus = currentImage.pose.head.get();
        double x = newFocus.getX() - currentFocus.getX();
        double y = newFocus.getY() - currentFocus.getY();
        actualOffset.setLocation(-x, -y);
        expectedOffset = new Point2D.Double(0.0, 0.0);
    }

    private void startAnimation(AnnotatedImage image, List<String> text) {
        long currentTimeMillis = System.currentTimeMillis();
        int transitionDuration = actualOffset.equals(expectedOffset) ? ZOOM_DURATION : TRANSITION_DURATION;
        pathx = new AnimationPath.Linear(actualOffset.getX(), expectedOffset.getX(), currentTimeMillis, transitionDuration);
        pathy = new AnimationPath.Linear(actualOffset.getY(), expectedOffset.getY(), currentTimeMillis, transitionDuration);
        pathz = new AnimationPath.Linear(actualZoom, expectedZoom, currentTimeMillis, transitionDuration);
        alpha = new AnimationPath.Linear(actualAlpha, expectedAlpha, currentTimeMillis, transitionDuration);
        host.setActorOffset(actualOffset);
        host.setActorZoom(actualZoom);
        host.setActorAlpha(actualAlpha);
        host.show(image, text);
        animator.notifyAll();
    }

    private void waitAnimationCompleted() {
        while (animationsRunning()) {
            try {
                animator.wait(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void setFocusLevel(float focusLevel) {
        host.setFocusLevel(focusLevel);
    }

    @Override
    public void setActorOffset(Point2D offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setActorZoom(double zoom) {
        synchronized (animator) {
            expectedZoom = zoom;
            pathz = new AnimationPath.Linear(actualZoom, expectedZoom, System.currentTimeMillis(), ZOOM_DURATION);
            animator.notifyAll();
        }
    }

    @Override
    public void setActorAlpha(float alpha) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void show() {
        host.show();
    }

    @Override
    public void showInterTitle(String text) {
        host.showInterTitle(text);
    }

    @Override
    public void endScene() {
        host.endScene();
    }

    @Override
    public List<Boolean> showItems(String caption, List<String> choices, List<Boolean> values, boolean allowCancel) throws InterruptedException {
        return host.showItems(caption, choices, values, allowCancel);
    }

    @Override
    public InputMethod inputMethod() {
        return host.inputMethod();
    }

    @Override
    public void setQuitHandler(Consumer<ScriptInterruptedEvent> onQuitHandler) {
        host.setQuitHandler(onQuitHandler);
    }

    @Override
    public File getLocation(Location folder) {
        return host.getLocation(folder);
    }

}
