package teaselib.core.ui;

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

    final Host host;

    private final Thread animator;
    private double actual = 1.0;
    private double expected = 1.0;
    private AnimationPath path;

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

    private final static long frameTimeMillis = 16;

    private void animate() {
        try {
            synchronized (animator) {
                while (!Thread.interrupted()) {
                    animator.wait();
                    while (expected != actual) {
                        long now = System.currentTimeMillis();
                        actual = path.get(now);
                        host.setActorZoom(actual);
                        host.show();
                        long finish = now;
                        long duration = frameTimeMillis - (finish - now);
                        if (duration > 0) {
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

    @Override
    public Persistence persistence(Configuration configuration) throws IOException {
        return host.persistence(configuration);
    }

    @Override
    public Audio audio(ResourceLoader resources, String path) {
        return host.audio(resources, path);
    }

    @Override
    public void show(AnnotatedImage actorImage, List<String> text) {
        host.show(actorImage, text);
    }

    @Override
    public void setFocusLevel(float focusLevel) {
        host.setFocusLevel(focusLevel);
    }

    @Override
    public void setActorZoom(double zoom) {
        synchronized (animator) {
            expected = zoom;
            path = new AnimationPath.Linear(actual, expected, System.currentTimeMillis(), 200);
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
