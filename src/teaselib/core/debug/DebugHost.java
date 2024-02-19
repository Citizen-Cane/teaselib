package teaselib.core.debug;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Closeable;
import teaselib.core.Persistence;
import teaselib.core.concurrency.NamedExecutorService;
import teaselib.core.configuration.Configuration;
import teaselib.core.ui.Choice;
import teaselib.core.ui.Choices;
import teaselib.core.ui.HostInputMethod;
import teaselib.core.ui.InputMethod;
import teaselib.core.ui.InputMethod.UiEvent;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;
import teaselib.host.Host;
import teaselib.util.AnnotatedImage;

/**
 * @author Citizen-Cane
 *
 */
public class DebugHost implements Host, HostInputMethod.Backend, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(DebugHost.class);

    private final NamedExecutorService executorService;
    private final InputMethod inputMethod;

    private final ReentrantLock replySection = new ReentrantLock(true);
    private final Condition click = replySection.newCondition();

    private List<Choice> currentChoices = Collections.emptyList();

    public final DebugPersistence persistence;
    private final UnaryOperator<Persistence> persistenceSupplier;

    public DebugHost() {
        this(p -> p);
    }

    public DebugHost(UnaryOperator<Persistence> persistenceSupplier) {
        this.persistence = new DebugPersistence(new DebugStorage());
        this.persistenceSupplier = persistenceSupplier;
        Thread.currentThread().setName(getClass().getSimpleName() + " main script thread");
        executorService = NamedExecutorService.singleThreadedQueue(HostInputMethod.class.getSimpleName());
        inputMethod = new HostInputMethod(executorService, this);
    }

    @Override
    public void close() {
        executorService.shutdown();
    }

    @Override
    public AudioSystem audioSystem() {
        return Host.AudioSystem.None;
    }

    @Override
    public Persistence persistence(Configuration configuration) {
        return persistenceSupplier.apply(persistence);
    }

    @Override
    public void setup() {
        //
    }

    @Override
    public void show(AnnotatedImage actorImage, List<String> text) {
        // Ignore
    }

    @Override
    public void setFocusLevel(float focusLevel) {
        // Ignore
    }

    @Override
    public void setActorZoom(double zoom) {
        // Ignore
    }

    @Override
    public void setTransition(Point2D prev, double prevZoom, Point2D cur, double nextZoom, float blend, float textBLendIn, float textBlendOut) {
        // Ignore
    }

    @Override
    public void show() {
        // Ignore
    }

    @Override
    public void showInterTitle(List<String> text) {
        // Ignore
    }

    @Override
    public void endScene() {
        // Ignore
    }

    @Override
    public List<Boolean> showItems(String caption, List<String> choices, List<Boolean> values, boolean allowCancel) {
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
    public void updateUI(UiEvent event) {
        // Ignore
    }

    @Override
    public boolean dismissChoices(List<Choice> choices) {
        if (logger.isTraceEnabled()) {
            logger.trace("Dismiss {} @ {}", choices, Thread.currentThread().getStackTrace()[1]);
        }

        replySection.lock();
        try {
            if (currentChoices.isEmpty()) {
                if (replySection.hasWaiters(click)) {
                    throw new IllegalStateException("Trying to dismiss without current choices: " + choices);
                }
                logger.trace("No current choices");
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
    public Prompt.Result reply(Choices choices) throws InterruptedException {
        logger.info("Reply {} @ {}", choices, Thread.currentThread().getStackTrace()[1]);

        replySection.lockInterruptibly();
        try {
            currentChoices = new ArrayList<>(choices);
            if (replySection.hasWaiters(click)) {
                throw new IllegalStateException("Reply not dismissed: " + choices);
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
    }

    @Override
    public void setQuitHandler(Consumer<Host.ScriptInterruptedEvent> onQuit) {
        // Ignore
    }

    @Override
    public File getLocation(Location folder) {
        if (folder == Location.Host)
            return new File("bin");
        else if (folder == Location.TeaseLib)
            return new File(".");
        else if (folder == Location.User)
            return new File("bin");
        else if (folder == Location.Log)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(Objects.toString(folder));
    }

    @Override
    public String toString() {
        return "[current choices = " + currentChoices + " , replaySection = "
                + (replySection.isLocked() ? "locked" : "free") + " ]";
    }

}
