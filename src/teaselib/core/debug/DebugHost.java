package teaselib.core.debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.bytedeco.javacpp.opencv_core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Audio;
import teaselib.core.Closeable;
import teaselib.core.Host;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.VideoRenderer;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.HostInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;

public class DebugHost implements Host, HostInputMethod.Backend, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(DebugHost.class);

    static final Point javacvDebugWindow = new Point(80, 80);

    private final NamedExecutorService executorService;
    private final InputMethod inputMethod;

    private final ReentrantLock replySection = new ReentrantLock(true);
    private final Condition click = replySection.newCondition();

    private List<Choice> currentChoices = Collections.emptyList();

    public DebugHost() {
        super();
        Thread.currentThread().setName(getClass().getSimpleName() + " main script thread");
        executorService = NamedExecutorService.singleThreadedQueue(HostInputMethod.class.getSimpleName());
        inputMethod = new HostInputMethod(executorService, this);
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    @Override
    public Audio audio(ResourceLoader resources, String path) {
        return new Audio() {
            @Override
            public void load() {
                // Ignore
            }

            @Override
            public void play() {
                // Ignore
            }

            @Override
            public void stop() {
                // Ignore
            }
        };
    }

    @Override
    public void show(byte[] imageBytes, String text) {
        // Ignore
    }

    @Override
    public void showInterTitle(String text) {
        // Ignore
    }

    @Override
    public void endScene() {
        // Ignore
    }

    @Override
    public List<Boolean> showCheckboxes(String caption, List<String> choices, List<Boolean> values,
            boolean allowCancel) {
        return new ArrayList<>(values);
    }

    private List<Runnable> getClickableChoices(List<Choice> choices) {
        List<Runnable> clickables = new ArrayList<>(choices.size());
        for (int i = 0; i < choices.size(); i++) {
            clickables.add(click::signal);
        }
        return clickables;
    }

    @Override
    public boolean dismissChoices(List<Choice> choices) {
        logger.info("Dismiss {} @ {}", choices, Thread.currentThread().getStackTrace()[1]);

        replySection.lock();
        try {
            if (currentChoices.isEmpty()) {
                if (replySection.hasWaiters(click)) {
                    throw new IllegalStateException("Trying to dismiss without current choices: " + choices);
                }
                logger.info("No current choices");
                return false;
            }

            if (!replySection.hasWaiters(click)) {
                logger.warn("Dismiss called on latch already counted down: {}", choices);
            } else {
                try {
                    Optional<Runnable> choice = getClickableChoices(choices).stream().findFirst();
                    if (choice.isPresent()) {
                        choice.get().run();
                    }
                } catch (Exception e) {
                    throw ExceptionUtil.asRuntimeException(e);
                }
                currentChoices = Collections.emptyList();

                if (replySection.hasWaiters(click)) {
                    throw new IllegalStateException("Dismiss failed to dismiss click condition: " + choices);
                }
            }

            return true;
        } finally {
            replySection.unlock();
        }
    }

    @Override
    public InputMethod inputMethod() {
        return inputMethod;
    }

    @Override
    public Prompt.Result reply(Choices choices) {
        logger.info("Reply {} @ {}", choices, Thread.currentThread().getStackTrace()[1]);

        try {
            replySection.lockInterruptibly();
            try {
                currentChoices = new ArrayList<>(choices);
                if (replySection.hasWaiters(click)) {
                    throw new IllegalStateException("Reply not dismissed: " + choices);
                }
                if (Thread.interrupted()) {
                    throw new ScriptInterruptedException();
                }

                while (!currentChoices.isEmpty()) {
                    click.await();
                }

                if (replySection.hasWaiters(click)) {
                    throw new IllegalStateException("Reply - still waiting on click");
                }

                return Prompt.Result.DISMISSED;
            } finally {
                currentChoices = Collections.emptyList();
                replySection.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }
    }

    @Override
    public void setQuitHandler(Runnable onQuit) {
        // Ignore
    }

    @Override
    public VideoRenderer getDisplay(Type displayType) {
        return new VideoRendererJavaCV(displayType) {
            @Override
            protected Point getPosition(Type type, int width, int height) {
                return javacvDebugWindow;
            }
        };
    }

    @Override
    public File getLocation(Location folder) {
        if (folder == Location.Host)
            return new File("bin");
        else if (folder == Location.TeaseLib)
            return new File(".");
        else if (folder == Location.User)
            throw new UnsupportedOperationException();
        else if (folder == Location.Log)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(Objects.toString(folder));
    }
}
