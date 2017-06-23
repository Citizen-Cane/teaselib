package teaselib.hosts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
        // TODO Auto-generated method stub
        return null;
    }

    int selectedIndex = 0;
    AtomicReference<CountDownLatch> latch = new AtomicReference<CountDownLatch>(new CountDownLatch(0));

    @Override
    public List<Delegate> getClickableChoices(List<String> choices) {
        List<Delegate> clickables = new ArrayList<Delegate>(choices.size());
        for (int i = 0; i < choices.size(); i++) {
            final int j = i;
            clickables.add(new Delegate() {
                @Override
                public void run() {
                    DummyHost.this.selectedIndex = j;
                    DummyHost.this.latch.get().countDown();
                }
            });
        }
        return clickables;
    }

    AtomicBoolean validateSingleEntrance = new AtomicBoolean(false);

    @Override
    public boolean dismissChoices(List<String> choices) {
        logger.info("Dismiss " + choices + " @ " + Thread.currentThread().getStackTrace()[1].toString());
        // Thread.dumpStack();

        // if (validateSingleEntrance.getAndSet(true)) {
        // throw new IllegalStateException("Dismiss multiple entry detected: " +
        // choices);
        // }

        try {

            // The weak point - must wait until there is something to be
            // dismissed,
            // since there is no guarantee when the host will be creating the
            // prompts.
            // Therefore dismissChoices() might be called before buttons are
            // realized.
            // The SexScripts host returns false until SS has created the
            // buttons,
            // so we have to, too.
            if (currentChoices.isEmpty()) {
                if (latch.get().getCount() > 0) {
                    throw new IllegalStateException("Trying to dismiss without current choices: " + choices);
                }
                return false;
            }

            // TODO triggers, although not fatal
            if (latch.get().getCount() == 0) {
                // throw new IllegalStateException("Dismiss called on latch
                // already counted down: " + choices);
            }

            List<Delegate> clickableChoices = getClickableChoices(choices);

            // notifyAll();

            for (Delegate delegate : clickableChoices) {
                delegate.run();
            }

            currentChoices = Collections.emptyList();

            if (latch.get().getCount() > 0) {
                throw new IllegalStateException("Dismiss failed to countdown active latch: " + choices);
            }

            return true;
        } finally {
            try {
                if (latch.get().getCount() > 0) {
                    throw new IllegalStateException("Reply - still waiting on Latch ");
                }
            } finally {
                // validateSingleEntrance.set(false);
            }
        }
    }

    @Override
    public int reply(List<String> choices) throws ScriptInterruptedException {
        logger.info("Reply " + choices + " @ " + Thread.currentThread().getStackTrace()[1].toString());
        // Thread.dumpStack();

        if (validateSingleEntrance.getAndSet(true)) {
            throw new IllegalStateException("Reply multiple entry detected: " + choices);
        }

        try {
            if (latch.get().getCount() > 0) {
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
                                    latch.getAndSet(new CountDownLatch(1)).countDown();
                                    // wait();
                                    latch.get().await();
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
            if (choices.size() == 1) {
                return 0;
            } else {
                throw new IllegalStateException("No rule to dismiss buttons matched for " + choices);
            }
        } finally {
            try {
                if (latch.get().getCount() > 0) {
                    throw new IllegalStateException("Reply - still waiting on Latch ");
                }
            } finally {
                validateSingleEntrance.set(false);
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

    public void setResponses(Map<String, Response> responses) {
        this.responses = responses;
    }

}
