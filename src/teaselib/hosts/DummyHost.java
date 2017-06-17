package teaselib.hosts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.bytedeco.javacpp.opencv_core.Point;

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

    @Override
    public boolean dismissChoices(List<String> choices) {
        for (Delegate delegate : getClickableChoices(choices)) {
            delegate.run();
        }
        currentChoices = Collections.emptyList();
        return true;
    }

    @Override
    public int reply(List<String> choices) throws ScriptInterruptedException {
        if (Thread.interrupted()) {
            throw new ScriptInterruptedException();
        }

        currentChoices = new ArrayList<String>(choices);
        latch.getAndSet(new CountDownLatch(1)).countDown();
        selectedIndex = 0;

        allChoices: for (Entry<String, Response> entry : responses.entrySet()) {
            Pattern choice = WildcardPattern.compile(entry.getKey());
            for (int i = 0; i < choices.size(); i++) {
                if (choice.matcher(choices.get(i)).matches()) {
                    if (entry.getValue().equals(Debugger.Response.Ignore)) {
                        try {
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
        return selectedIndex;
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
