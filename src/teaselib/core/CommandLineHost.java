package teaselib.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.bytedeco.javacpp.opencv_core.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.VideoRenderer.Type;
import teaselib.core.debug.DebugHost;
import teaselib.core.javacv.VideoRendererJavaCV;
import teaselib.core.ui.Choice;
import teaselib.core.ui.InputMethod;

public class CommandLineHost implements Host {
    private static final Logger logger = LoggerFactory.getLogger(DebugHost.class);

    static final Point javacvDebugWindow = new Point(80, 80);

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

    int selectedIndex = 0;

    final ReentrantLock replySection = new ReentrantLock(true);
    final Condition click = replySection.newCondition();

    private List<Runnable> getClickableChoices(List<Choice> choices) {
        List<Runnable> clickables = new ArrayList<>(choices.size());
        for (int i = 0; i < choices.size(); i++) {
            final int j = i;
            clickables.add(() -> {
                selectedIndex = j;
                click.signal();
            });
        }
        return clickables;
    }

    @Override
    public InputMethod inputMethod() {
        throw new UnsupportedOperationException();
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
            return new File("").getAbsoluteFile();
        else if (folder == Location.TeaseLib)
            return new File(getLocation(Location.Host), "lib" + File.separator + "TeaseLib" + File.separator);
        else if (folder == Location.User)
            return new File(getLocation(Location.Host).getAbsoluteFile(), "teaselib");
        else if (folder == Location.Log)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(folder.toString());
    }
}
