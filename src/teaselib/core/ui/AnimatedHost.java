package teaselib.core.ui;

import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

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

    // code paths for transitions and zoom-ionly
    private static final int ZOOM_DURATION = 400;
    private static final int TRANSITION_DURATION = 1000;

    private final static long FRAMETIME_MILLIS = 16;

    final Host host;
    private final Thread animator;

    private AnnotatedImage currentImage = AnnotatedImage.NoImage;

    private double actualZoom = 1.0;
    private double expectedZoom = 1.0;

    private Point2D actualOffset = new Point2D.Double();
    private Point2D expectedOffset = new Point2D.Double();
    private AnimationPath pathx = AnimationPath.NONE;
    private AnimationPath pathy = AnimationPath.NONE;
    private AnimationPath pathz;

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
        try {
            synchronized (animator) {
                while (!Thread.interrupted()) {
                    animator.wait();
                    animator.wait(FRAMETIME_MILLIS);
                    while (animationsRunning()) {
                        long now = System.currentTimeMillis();
                        actualOffset.setLocation(pathx.get(now), pathy.get(now));
                        actualZoom = pathz.get(now);
                        host.setActorOffset(actualOffset);
                        host.setActorZoom(actualZoom);
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
                }
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    private boolean animationsRunning() {
        return expectedZoom != actualZoom || expectedOffset.getX() != actualOffset.getX() || expectedOffset.getY() != actualOffset.getY();
    }

    @Override
    public Persistence persistence(Configuration configuration) throws IOException {
        return host.persistence(configuration);
    }

    @Override
    public Audio audio(ResourceLoader resources, String path) {
        return host.audio(resources, path);
    }

    // Daisy WAtts: 3rd image with offset - does not cover surface

    @Override
    public void show(AnnotatedImage newImage, List<String> text) {
        synchronized (animator) {
            waitAnimationCompleted();

            float currentDistance = currentImage.pose.distance.orElse(0.0f);
            float newDistance = newImage.pose.distance.orElse(0.0f);

            if (newDistance != 0.0f && newDistance < currentDistance) {
                // New image nearer
                if (actualZoom > 1.0) {
                    // current image zoomed
                    skipUnzoomAfterPrompt(newImage);
                } else {
                    // TODO Results in the original image moved over the original image - looks stupid
                    // TODO zoom actor farer away since this makes distance transitions more realistic
                    // zoomBeforeDisplayingNewImage(newImage, text, currentDistance, newDistance);

                    // expectedZoom = 1.0;
                    zoomTo(newImage);
                    // TODO sofa test images: slide-in from tight does not work
                }
            } else if (currentDistance != 0.0f && newDistance > currentDistance) {
                // new image farer
                displayImageWithZoomToMatchCurrentDistance(newImage, currentDistance, newDistance);
            } else {
                translateToOrigin();
            }

            currentImage = newImage;
            startAnimation(currentImage, text);
        }
    }

    private void displayImageWithZoomToMatchCurrentDistance(AnnotatedImage newImage, float currentDistance, float newDistance) {
        actualZoom = newDistance / currentDistance;
        zoomTo(newImage);
    }

    private void zoomBeforeDisplayingNewImage(AnnotatedImage newImage, List<String> text, float currentDistance, float newDistance) {
        // TODO any focus region
        Point2D newFocus = newImage.pose.head.get();
        Point2D currentFocus = currentImage.pose.head.get();
        expectedOffset.setLocation(newFocus.getX() - currentFocus.getX(), newFocus.getY() - currentFocus.getY());
        expectedZoom = currentDistance / newDistance;

        startAnimation(currentImage, text);
        animator.notifyAll();
        waitAnimationCompleted();

        actualOffset = new Point2D.Double(0.0, 0.0);
        expectedOffset = new Point2D.Double(0.0, 0.0);
        actualZoom = 1.0;
        expectedZoom = 1.0;
    }

    private void skipUnzoomAfterPrompt(AnnotatedImage newImage) {
        actualZoom = 1.0;
        expectedZoom = 1.0;
        zoomTo(newImage);
    }

    private void zoomTo(AnnotatedImage newImage) {
        // TODO any focus region
        Point2D newFocus = newImage.pose.head.get();
        Point2D currentFocus = currentImage.pose.head.get();
        // TODO wrong : head on right sides in from too far left - heads don't match
        actualOffset.setLocation(-(newFocus.getX() - currentFocus.getX()), -(newFocus.getY() - currentFocus.getY()));
        expectedOffset = new Point2D.Double(0.0, 0.0);
    }

    private void translateToOrigin() {
        expectedOffset = new Point2D.Double(0.0, 0.0);
        expectedZoom = 1.0;
    }

    private void startAnimation(AnnotatedImage image, List<String> text) {
        long currentTimeMillis = System.currentTimeMillis();
        pathx = new AnimationPath.Linear(actualOffset.getX(), expectedOffset.getX(), currentTimeMillis, TRANSITION_DURATION);
        pathy = new AnimationPath.Linear(actualOffset.getY(), expectedOffset.getY(), currentTimeMillis, TRANSITION_DURATION);
        pathz = new AnimationPath.Linear(actualZoom, expectedZoom, currentTimeMillis, TRANSITION_DURATION);
        host.setActorOffset(actualOffset);
        host.setActorZoom(actualZoom);
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
