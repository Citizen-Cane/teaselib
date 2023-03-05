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
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Persistence;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.concurrency.NoFuture;
import teaselib.core.configuration.Configuration;
import teaselib.host.Host;
import teaselib.host.Scene;
import teaselib.util.AnnotatedImage;

/**
 * @author Citizen-Cane
 *
 */
public class AnimatedHost implements Host, Closeable {

    static final Logger logger = LoggerFactory.getLogger(AnimatedHost.class);

    private static final int ZOOM_DURATION = 200;
    private static final int TRANSITION_DURATION = 500;
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
    private final NamedExecutorService animator;
    private Future<?> animationTask = NoFuture.Void;

    private AnnotatedImage currentImage = AnnotatedImage.NoImage;
    private boolean isIntertitle = false;

    static class ActorPath {
        double actualZoom = 1.0;
        double expectedZoom = 1.0;
        Point2D actualOffset = new Point2D.Double();
        Point2D expectedOffset = new Point2D.Double();

        private AnimationPath pathx;
        private AnimationPath pathy;
        private AnimationPath pathz;

        void move(Animation animation, long currentTimeMillis, int transitionDuration) {
            if (animation.type.contains(Animation.Type.MoveNew)) {
                pathx = new AnimationPath.Linear(actualOffset.getX(), expectedOffset.getX(), currentTimeMillis,
                        transitionDuration);
                pathy = new AnimationPath.Linear(actualOffset.getY(), expectedOffset.getY(), currentTimeMillis,
                        transitionDuration);
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

    final ActorPath previous = new ActorPath();
    final ActorPath current = new ActorPath();

    static class AlphaBlend {
        float actual = 0.0f;
        float expected = 1.0f;
        private AnimationPath alphapath;

        void blend(Animation animation, long currentTimeMillis, int blendMillis) {
            if (animation.type.contains(Animation.Type.Blend)) {
                alphapath = new AnimationPath.Linear(actual, expected, currentTimeMillis, blendMillis);
            } else {
                alphapath = new AnimationPath.Constant(expected);
            }
        }

        public void blend(Animation animation, long currentTimeMillis, int delayMillis, int blendMillis) {
            if (animation.type.contains(Animation.Type.Blend)) {
                alphapath = new AnimationPath.Delay(delayMillis,
                        new AnimationPath.Linear(actual, expected, currentTimeMillis, blendMillis));
            } else {
                alphapath = new AnimationPath.Constant(expected);
            }
        }

        void advance(long now) {
            actual = (float) alphapath.get(now);
        }
    }

    final AlphaBlend sceneBlend = new AlphaBlend();
    final AlphaBlend currentTextBlend = new AlphaBlend();
    final AlphaBlend previoustextBlend = new AlphaBlend();

    private final Scene scene;

    public AnimatedHost(Host host, Scene scene) {
        this.host = host;
        this.scene = scene;
        this.animator = NamedExecutorService.sameThread("Animate UI");
        setAnimationPaths(Animation.None, 0, 0);
    }

    @Override
    public void close() throws IOException {
        animator.shutdown();
        if (host instanceof Closeable closeable) {
            closeable.close();
        }
    }

    private void animate() {
        synchronized (animator) {
            try {
                animator.wait(FRAMETIME_MILLIS);
                do {
                    long now = System.currentTimeMillis();
                    previous.advance(now);
                    current.advance(now);
                    sceneBlend.advance(now);
                    currentTextBlend.advance(now);
                    previoustextBlend.advance(now);
                    setTransition();
                    host.show();
                    long finish = System.currentTimeMillis();
                    long duration = FRAMETIME_MILLIS - (finish - now);
                    if (duration > 0) {
                        animator.wait(duration);
                    }
                } while (animationsRunning());
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private boolean animationsRunning() {
        return previous.expectedZoom != previous.actualZoom ||
                current.expectedZoom != current.actualZoom ||
                sceneBlend.actual != sceneBlend.expected ||
                currentTextBlend.actual != currentTextBlend.expected ||
                previoustextBlend.actual != previoustextBlend.expected ||
                previous.expectedOffset.getX() != previous.actualOffset.getX() ||
                current.expectedOffset.getY() != current.actualOffset.getY();
    }

    @Override
    public AudioSystem audioSystem() {
        return host.audioSystem();
    }

    @Override
    public Persistence persistence(Configuration configuration) throws IOException {
        return host.persistence(configuration);
    }

    @Override
    public void show(AnnotatedImage newImage, List<String> text) {
        synchronized (animator) {
            animationTask.cancel(true);
            // Must be set first in order to fetch transition vector later on
            host.show(newImage, text);

            float currentDistance = currentImage != null ? currentImage.pose.distance.orElse(0.0f) : 0.0f;
            float newDistance = newImage != null ? newImage.pose.distance.orElse(0.0f) : 0.0f;

            Optional<Point2D> currentFocusRegion = currentImage != null ? currentImage.pose.head : Optional.empty();
            Optional<Point2D> newFocusRegion = newImage != null ? newImage.pose.head : Optional.empty();
            boolean sameRegion = true && currentFocusRegion.isPresent() && newFocusRegion.isPresent();
            if (sameRegion) {
                if (newDistance != 0.0f && currentDistance != 0.0f) {
                    zoomToDistance(currentDistance, newDistance);
                } else {
                    previous.actualZoom = current.actualZoom;
                    previous.expectedZoom = current.expectedZoom;
                }
                translateFocus(currentFocusRegion.get(), newFocusRegion.get());
            } else {
                translateFocusToOrigin();
            }
            current.expectedZoom = 1.0;
            sceneBlend.actual = 0.0f;

            previoustextBlend.actual = currentTextBlend.actual;
            previoustextBlend.expected = 0.0f;
            currentTextBlend.actual = 0.0f;
            currentTextBlend.expected = 1.0f;
            setTransition();

            currentImage = newImage;
            isIntertitle = false;

            long currentTimeMillis = System.currentTimeMillis();
            int transitionDuration = current.actualOffset.equals(current.expectedOffset) ? ZOOM_DURATION
                    : TRANSITION_DURATION;
            setAnimationPaths(Animation.MoveBoth, currentTimeMillis, transitionDuration);
        }
    }

    private void zoomToDistance(float currentDistance, float newDistance) {
        previous.actualZoom = current.actualZoom;
        previous.expectedZoom = currentDistance / newDistance;
        current.actualZoom = newDistance / currentDistance;
    }

    private void translateFocusToOrigin() {
        previous.expectedOffset = new Point2D.Double(0.0, 0.0);
        current.expectedOffset = new Point2D.Double(0.0, 0.0);
    }

    private void translateFocus(Point2D currentFocusRegion, Point2D newFocusRegion) {
        previous.actualOffset = new Point2D.Double(0.0, 0.0);
        current.expectedOffset = new Point2D.Double(0.0, 0.0);
        Point2D transition = scene.getTransitionVector(currentFocusRegion, newFocusRegion);
        current.actualOffset = new Point2D.Double(-transition.getX(), -transition.getY());
        previous.expectedOffset = transition;
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

        if (transitionDuration > ZOOM_DURATION * 2) {
            sceneBlend.blend(animation, currentTimeMillis, (TRANSITION_DURATION - ZOOM_DURATION * 2) / 2,
                    ZOOM_DURATION * 2);
        } else {
            sceneBlend.blend(animation, currentTimeMillis, transitionDuration);
        }

        previoustextBlend.blend(animation, currentTimeMillis, ZOOM_DURATION);
        currentTextBlend.blend(animation, currentTimeMillis, TRANSITION_DURATION - ZOOM_DURATION, ZOOM_DURATION);
    }

    @Override
    public void setFocusLevel(float focusLevel) {
        host.setFocusLevel(focusLevel);
    }

    @Override
    public void setActorZoom(double zoom) {
        synchronized (animator) {
            if (zoom != current.expectedZoom) {
                animationTask.cancel(true);
                current.expectedZoom = zoom;
                current.pathz = new AnimationPath.Linear(
                        current.actualZoom, current.expectedZoom,
                        System.currentTimeMillis(), ZOOM_DURATION);
            }
        }
    }

    @Override
    public void setTransition(Point2D prev, double prevZoom, Point2D cur, double nextZoom, float blend,
            float textBLendIn, float textBlendOut) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void show() {
        synchronized (animator) {
            if (animationTask.isCancelled() || animationTask.isDone()) {
                animationTask = animator.submit(this::animate);
            }
        }
    }

    @Override
    public void showInterTitle(String text) {
        synchronized (animator) {
            animationTask.cancel(true);
            host.showInterTitle(text);
            if (!isIntertitle) {
                previoustextBlend.actual = currentTextBlend.actual;
                previoustextBlend.expected = 0.0f;
                currentTextBlend.actual = 0.0f;
                currentTextBlend.expected = 1.0f;
                setTransition();

                long currentTimeMillis = System.currentTimeMillis();
                Animation animation = Animation.BlendIn;
                setIntertitleTextBlend(animation, currentTimeMillis);

                isIntertitle = true;
            } else {
                previoustextBlend.expected = 0.0f;
                currentTextBlend.expected = 1.0f;
                setTransition();
                long currentTimeMillis = System.currentTimeMillis();
                previoustextBlend.blend(Animation.None, currentTimeMillis, 0);
                currentTextBlend.blend(Animation.None, currentTimeMillis, 0);
            }
        }
    }

    private void setIntertitleTextBlend(Animation animation, long currentTimeMillis) {
        if (previoustextBlend.actual > 0.0) {
            previoustextBlend.blend(animation, currentTimeMillis, ZOOM_DURATION);
            currentTextBlend.blend(animation, currentTimeMillis, ZOOM_DURATION, ZOOM_DURATION);
        } else {
            previoustextBlend.blend(Animation.None, currentTimeMillis, 0);
            currentTextBlend.blend(animation, currentTimeMillis, ZOOM_DURATION);
        }
    }

    @Override
    public void endScene() {
        synchronized (animator) {
            animationTask.cancel(true);
            host.endScene();

            previoustextBlend.actual = currentTextBlend.actual;
            previoustextBlend.expected = 0.0f;
            currentTextBlend.expected = 0.0f;
            setTransition();

            long currentTimeMillis = System.currentTimeMillis();
            previoustextBlend.blend(Animation.BlendIn, currentTimeMillis, ZOOM_DURATION);
            currentTextBlend.blend(Animation.None, currentTimeMillis, 0);
        }
    }

    private void setTransition() {
        host.setTransition(
                previous.actualOffset, previous.actualZoom,
                current.actualOffset, current.actualZoom,
                sceneBlend.actual, currentTextBlend.actual, previoustextBlend.actual);
    }

    @Override
    public List<Boolean> showItems(String caption, List<String> choices, List<Boolean> values, boolean allowCancel)
            throws InterruptedException {
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
