package teaselib.core;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import teaselib.core.ai.perception.HumanPose;
import teaselib.core.configuration.Configuration;
import teaselib.core.ui.InputMethod;
import teaselib.core.util.FileUtilities;
import teaselib.util.AnnotatedImage;

public class CommandLineHost implements Host {

    @Override
    public Persistence persistence(Configuration configuration) {
        throw new UnsupportedOperationException();
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
    public void show(AnnotatedImage actorImage, List<String> text) {
        // Ignore
    }

    @Override
    public void setFocusLevel(float focusLevel) {
        // Ignore
    }

    @Override
    public void show() {
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
    public List<Boolean> showItems(String caption, List<String> choices, List<Boolean> values,
            boolean allowCancel) {
        return new ArrayList<>(values);
    }

    int selectedIndex = 0;

    final ReentrantLock replySection = new ReentrantLock(true);
    final Condition click = replySection.newCondition();

    @Override
    public InputMethod inputMethod() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setQuitHandler(Consumer<Host.ScriptInterruptedEvent> onQuit) {
        // Ignore
    }

    @Override
    public File getLocation(Location folder) {
        if (folder == Location.Host)
            return FileUtilities.currentDir();
        else if (folder == Location.TeaseLib)
            return ResourceLoader.getProjectPath(getClass()).getParentFile().getAbsoluteFile();
        else if (folder == Location.User)
            return new File(getLocation(Location.Host).getAbsoluteFile(), "teaselib");
        else if (folder == Location.Log)
            return getLocation(Location.Host);
        else
            throw new IllegalArgumentException(Objects.toString(folder));
    }

    @Override
    public void setGaze(Point2D gaze) {
        // Ignore

    }

    @Override
    public void setActorProximity(HumanPose.Proximity proximity) {
        // Ignore
    }
}
