package teaselib.hosts;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

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
import teaselib.core.util.WildcardPattern;

public class DummyHost implements Host {
    private static final Logger logger = LoggerFactory.getLogger(DummyHost.class);

    static final Point javacvDebugWindow = new Point(80, 80);

    private Map<String, Response> responses;
    private List<String> currentChoices = Collections.emptyList();

    public DummyHost() {
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

        try {
            replySection.lockInterruptibly();
            if (currentChoices.isEmpty()) {
                if (replySection.hasWaiters(click)) {
                    throw new IllegalStateException("Trying to dismiss without current choices: " + choices);
                }
                logger.info("No current choices");
                return false;
            }

            if (!replySection.hasWaiters(click)) {
                // throw new IllegalStateException("Dismiss called on latch
                // already counted down: " + choices);
            }

            for (Delegate delegate : getClickableChoices(choices)) {
                delegate.run();
            }
            currentChoices = Collections.emptyList();

            if (replySection.hasWaiters(click)) {
                throw new IllegalStateException("Dismiss failed to dismiss click condition: " + choices);
            }

            return true;
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } finally {
            replySection.unlock();
        }
    }

    @Override
    public int reply(List<String> choices) throws ScriptInterruptedException {
        logger.info("Reply " + choices + " @ " + Thread.currentThread().getStackTrace()[1].toString());

        try {
            replySection.lockInterruptibly();
            if (replySection.hasWaiters(click)) {
                throw new IllegalStateException("Reply not dismissed: " + choices);
            }
            if (Thread.interrupted()) {
                throw new ScriptInterruptedException();
            }
            currentChoices = new ArrayList<String>(choices);
            selectedIndex = 0;
            try {
                allChoices: for (Entry<String, Response> entry : responses.entrySet()) {
                    Pattern choice = WildcardPattern.compile(entry.getKey());
                    for (int i = 0; i < choices.size(); i++) {
                        if (choice.matcher(choices.get(i)).matches()) {
                            if (entry.getValue().equals(Debugger.Response.Ignore)) {
                                try {
                                    logger.info("Awaiting dismiss for" + choices.get(i));
                                    click.await();
                                    break allChoices;
                                } catch (InterruptedException e) {
                                    throw new ScriptInterruptedException();
                                }
                            } else {
                                return i;
                            }
                        }
                    }
                }
            } finally {
                currentChoices = Collections.emptyList();
            }

            if (replySection.hasWaiters(click)) {
                throw new IllegalStateException("Reply - still waiting on click");
            }

            if (choices.size() == 1) {
                return 0;
            } else {
                throw new IllegalStateException("No rule to dismiss buttons matched for " + choices);
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        } finally {
            replySection.unlock();
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

    public void setResponses(Map<String, Response> responses) {
        this.responses = responses;
    }

    @Override
    public File getLocation(Location folder) {
        if (folder == Location.Host)
            return new File("bin.test/");
        else if (folder == Location.TeaseLib)
            return new File(".");
        else if (folder == Location.User)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(folder.toString());
    }

}
