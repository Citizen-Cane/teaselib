package teaselib.core.debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.bytedeco.javacpp.opencv_core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.Debugger;
import teaselib.core.Debugger.Response;
import teaselib.core.Host;
import teaselib.core.ResourceLoader;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.VideoRenderer;
import teaselib.core.VideoRenderer.Type;
import teaselib.core.events.Delegate;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.core.ui.Prompt;
import teaselib.core.util.ExceptionUtil;

public class DebugHost implements Host {
    private static final Logger logger = LoggerFactory.getLogger(DebugHost.class);

    static final Point javacvDebugWindow = new Point(80, 80);

    private List<String> currentChoices = Collections.emptyList();

    public DebugHost() {
        super();
        Thread.currentThread().setName(getClass().getSimpleName() + " Script");
    }

    @Override
    public void playSound(ResourceLoader resources, String path) throws IOException, InterruptedException {
    }

    @Override
    public Object playBackgroundSound(ResourceLoader resources, String path) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void stopSound(Object handle) {
        // TODO Auto-generated method stub

    }

    @Override
    public void show(byte[] imageBytes, String text) {
        // TODO Auto-generated method stub

    }

    @Override
    public void showInterTitle(String text) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<Boolean> showCheckboxes(String caption, List<String> choices, List<Boolean> values,
            boolean allowCancel) {
        return new ArrayList<Boolean>(values);
    }

    int selectedIndex = 0;

    final ReentrantLock replySection = new ReentrantLock(true);
    final Condition click = replySection.newCondition();

    private List<Delegate> getClickableChoices(List<String> choices) {
        List<Delegate> clickables = new ArrayList<Delegate>(choices.size());
        for (int i = 0; i < choices.size(); i++) {
            final int j = i;
            clickables.add(new Delegate() {
                @Override
                public void run() {
                    selectedIndex = j;
                    click.signal();
                }
            });
        }
        return clickables;
    }

    @Override
    public boolean dismissChoices(List<String> choices) {
        logger.info("Dismiss " + choices + " @ " + Thread.currentThread().getStackTrace()[1].toString());

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
                logger.warn("Dismiss called on latch already counted down: " + choices);
            } else {
                for (Delegate delegate : getClickableChoices(choices)) {
                    try {
                        delegate.run();
                    } catch (Exception e) {
                        throw ExceptionUtil.asRuntimeException(e);
                    }
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

    static class Reply {
        final int index;
        final Debugger.Response response;
        final String match;

        Reply(int index, Response response, String match) {
            this.index = index;
            this.response = response;
            this.match = match;
        }
    }

    @Override
    public int reply(List<String> choices) throws ScriptInterruptedException {
        logger.info("Reply " + choices + " @ " + Thread.currentThread().getStackTrace()[1].toString());

        try {
            currentChoices = new ArrayList<String>(choices);
            replySection.lockInterruptibly();
            if (replySection.hasWaiters(click)) {
                throw new IllegalStateException("Reply not dismissed: " + choices);
            }
            if (Thread.interrupted()) {
                throw new ScriptInterruptedException();
            }

            click.await();
            return Prompt.DISMISSED;
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException(e);
        } finally {
            currentChoices = Collections.emptyList();

            try {
                if (replySection.hasWaiters(click)) {
                    throw new IllegalStateException("Reply - still waiting on click");
                }
            } finally {
                replySection.unlock();
            }
        }
    }

    @Override
    public void setQuitHandler(Runnable onQuit) {
        // TODO Auto-generated method stub
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
            return getLocation(Location.Host);
        else if (folder == Location.User)
            return getLocation(Location.Host);
        else if (folder == Location.Log)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(folder.toString());
    }
}
