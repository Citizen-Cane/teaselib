package teaselib.core.ui;

import java.awt.geom.Point2D;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private static final int TRANSITION_DURATION = 1000;
    private final static long FRAMETIME_MILLIS = 16;

    enum Animation {

        None(),
        BlendIn(Type.Blend),
        Move(Type.Blend, Type.MoveNew, Type.Zoom),
        MoveBoth(Type.Blend, Type.MovePrevious, Type.MoveNew, Type.Zoom)

        ;

        enum Type {
            Blend,
            MovePrevious,
            MoveNew,
            Zoom
        }

        Set<Type> type;

        private Animation(Type... types) {
            this.type = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(types)));
        }
    }

    final Host host;
    private final Thread animator;

    private AnnotatedImage currentImage = AnnotatedImage.NoImage;

    static class ActorPath {
        double actualZoom = 1.0;
        double expectedZoom = 1.0;
        Point2D actualOffset = new Point2D.Double();
        Point2D expectedOffset = new Point2D.Double();

        AnimationPath pathx;
        AnimationPath pathy;
        AnimationPath pathz;

        void move(Animation animation, long currentTimeMillis, int transitionDuration) {
            if (animation.type.contains(Animation.Type.MoveNew)) {
                pathx = new AnimationPath.Linear(actualOffset.getX(), expectedOffset.getX(), currentTimeMillis, transitionDuration);
                pathy = new AnimationPath.Linear(actualOffset.getY(), expectedOffset.getY(), currentTimeMillis, transitionDuration);
            } else {
                pathx = new AnimationPath.Constant(expectedOffset.getX());
                pathy = new AnimationPath.Constant(expectedOffset.getY());
            }
        }

        void zoom(Animation animation, long currentTimeMillis, int transitionDuration) {
            if (animation.type.contains(Animation.Type.Zoom)) {
                pathz = new AnimationPath.Linear(actualZoom, expectedZoom, currentTimeMillis, transitionDuration);
            } else {
                pathz = new AnimationPath.Constant(expectedZoom);
            }
        }

        void advance(long now) {
            actualOffset.setLocation(pathx.get(now), pathy.get(now));
            actualZoom = pathz.get(now);
        }

    }

    ActorPath previous = new ActorPath();
    ActorPath current = new ActorPath();

    private float actualAlpha = 1.0f;
    private float expectedAlpha = 1.0f;

    private AnimationPath alpha;

    public AnimatedHost(Host host) {
        this.host = host;
        this.animator = new Thread(this::animate, "Animate UI");
        this.animator.start();
        setAnimationPaths(Animation.None, 0, 0);
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
                        previous.advance(now);
                        current.advance(now);
                        actualAlpha = (float) alpha.get(now);
                        host.setActorOffset(previous.actualOffset, current.actualOffset);
                        host.setPreviousActorImageZoom(previous.actualZoom);
                        host.setActorZoom(current.actualZoom);
                        host.setActorAlpha(actualAlpha);
                        host.show();
                        long finish = System.currentTimeMillis();
                        long duration = FRAMETIME_MILLIS - (finish - now);
                        if (duration > 0) {
                            animator.wait(duration);
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
        return current.expectedZoom != current.actualZoom ||
                expectedAlpha != actualAlpha ||
                current.expectedOffset.getX() != current.actualOffset.getX() ||
                current.expectedOffset.getY() != current.actualOffset.getY();
    }

    @Override
    public Persistence persistence(Configuration configuration) throws IOException {
        return host.persistence(configuration);
    }

    @Override
    public Audio audio(ResourceLoader resources, String path) {
        return host.audio(resources, path);
    }

    // TODO review transitions between Portait & Landscape and vice versa. Might be the cause of offset-images

    @Override
    public void show(AnnotatedImage newImage, List<String> text) {
        synchronized (animator) {
            waitAnimationCompleted();

            float currentDistance = currentImage != null ? currentImage.pose.distance.orElse(0.0f) : 0.0f;
            float newDistance = newImage != null ? newImage.pose.distance.orElse(0.0f) : 0.0f;

            Optional<Point2D> currentFocusRegion = currentImage != null ? currentImage.pose.head : Optional.empty();
            Optional<Point2D> newFocusRegion = newImage != null ? newImage.pose.head : Optional.empty();
            boolean sameRegion = true && currentFocusRegion.isPresent() && newFocusRegion.isPresent();
            if (sameRegion) {
                if (newDistance != 0.0f && newDistance < currentDistance) {
                    // -> New image nearer
                    if (current.actualZoom >= currentDistance / newDistance) {
                        // -> current image zoomed and new image can can be zoomed to match current focus region size
                        skipUnzoom(currentDistance, newDistance);
                        translateFocus(currentFocusRegion.get(), newFocusRegion.get());
                    } else {
                        translateToNearerDistance(currentDistance, newDistance);
                        translateFocus(currentFocusRegion.get(), newFocusRegion.get());
                    }
                } else if (currentDistance != 0.0f && newDistance > currentDistance) {
                    // -> new image farer - translate new image starting as zoomed as current image
                    translateToFarerDistance(currentDistance, newDistance);
                    translateFocus(currentFocusRegion.get(), newFocusRegion.get());
                } else {
                    previous.actualZoom = current.actualZoom;
                    previous.expectedZoom = current.expectedZoom;
                    translateFocus(currentFocusRegion.get(), newFocusRegion.get());
                }
            } else {
                translateFocusToOrigin();
            }
            current.expectedZoom = 1.0;
            actualAlpha = 0.0f;
            currentImage = newImage;
            start(Animation.MoveBoth, currentImage, text);
        }
    }

    private void skipUnzoom(float currentDistance, float newDistance) {
        // previous.expectedZoom = currentDistance / newDistance;
        current.actualZoom = Math.min(1.0, current.actualZoom * newDistance / currentDistance);
        // TODO actor zoom should only be reseted here, and when dismissing an answer
        // - but without synchronization between script and animation
        // - there's a small stutter caused by the script attempting to zoom out during animations
        // -> reset zoom (see code above) as dismissing a prompt is followed by displaying a new image
    }

    private void translateToNearerDistance(float currentDistance, float newDistance) {
        previous.actualZoom = current.actualZoom;
        previous.expectedZoom = currentDistance / newDistance;
        current.actualZoom = newDistance / currentDistance;
    }

    private void translateToFarerDistance(float currentDistance, float newDistance) {
        previous.actualZoom = current.actualZoom;
        previous.expectedZoom = currentDistance / newDistance;
        current.actualZoom = newDistance / currentDistance;
    }

    private void translateFocusToOrigin() {
        previous.expectedOffset = new Point2D.Double(0.0, 0.0);
        current.expectedOffset = new Point2D.Double(0.0, 0.0);
    }

    private void translateFocus(Point2D currentFocusRegion, Point2D newFocusRegion) {
        double x = newFocusRegion.getX() - currentFocusRegion.getX();
        double y = newFocusRegion.getY() - currentFocusRegion.getY();
        current.actualOffset = new Point2D.Double(-x, -y);
        current.expectedOffset = new Point2D.Double(0.0, 0.0);
        // previous location of focus region in current
        previous.actualOffset = new Point2D.Double(0.0, 0.0);
        previous.expectedOffset = new Point2D.Double(x, y);
    }

    private void start(Animation animation, AnnotatedImage image, List<String> text) {
        long currentTimeMillis = System.currentTimeMillis();
        int transitionDuration = current.actualOffset.equals(current.expectedOffset) ? ZOOM_DURATION : TRANSITION_DURATION;
        setAnimationPaths(animation, currentTimeMillis, transitionDuration);
        host.setActorOffset(previous.actualOffset, current.actualOffset);
        host.setActorZoom(current.actualZoom);
        host.setActorAlpha(actualAlpha);
        host.show(image, text);
        animator.notifyAll();
    }

    private void setAnimationPaths(Animation animation, long currentTimeMillis, int transitionDuration) {
        if (animation.type.contains(Animation.Type.MovePrevious)) {
            previous.move(animation, currentTimeMillis, transitionDuration);
            previous.zoom(animation, currentTimeMillis, transitionDuration);
        } else {
            previous.expectedOffset = new Point2D.Double(0.0, 0.0);
            previous.expectedZoom = 1.0f;
            previous.move(Animation.None, currentTimeMillis, transitionDuration);
            previous.zoom(Animation.None, currentTimeMillis, transitionDuration);
        }
        current.move(animation, currentTimeMillis, transitionDuration);
        current.zoom(animation, currentTimeMillis, transitionDuration);

        if (animation.type.contains(Animation.Type.Blend)) {
            alpha = new AnimationPath.Linear(actualAlpha, expectedAlpha, currentTimeMillis, transitionDuration);
        } else {
            alpha = new AnimationPath.Constant(expectedAlpha);
        }
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
    public void setActorOffset(Point2D previousImage, Point2D currentImage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPreviousActorImageZoom(double zoom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setActorZoom(double zoom) {
        synchronized (animator) {
            current.expectedZoom = zoom;
            current.pathz = new AnimationPath.Linear(current.actualZoom, current.expectedZoom, System.currentTimeMillis(), ZOOM_DURATION);
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
