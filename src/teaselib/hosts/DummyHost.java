package teaselib.hosts;

import java.awt.Image;
import java.io.IOException;
import java.util.List;

import teaselib.core.Host;
import teaselib.core.ResourceLoader;
import teaselib.core.events.Delegate;

public class DummyHost implements Host {

    @Override
    public void playSound(ResourceLoader resources, String path)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public Object playBackgroundSound(ResourceLoader resources, String path)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void stopSounds() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stopSound(Object handle) {
        // TODO Auto-generated method stub

    }

    @Override
    public void show(Image image, String text) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Boolean> showCheckboxes(String caption, List<String> choices,
            List<Boolean> values, boolean allowCancel) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Delegate> getClickableChoices(List<String> choices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int reply(List<String> choices) {
        // TODO Auto-generated method stub
        return 0;
    }
}
