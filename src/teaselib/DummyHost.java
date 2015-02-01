package teaselib;

import java.awt.Image;
import java.io.InputStream;
import java.util.List;

import teaselib.util.Delegate;

public class DummyHost implements Host {

    @Override
    public int getRandom(int min, int max) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void log(String line) {
        System.out.println(line);

    }

    @Override
    public void playSound(String path, InputStream inputStream) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object playBackgroundSound(String path, InputStream inputStream) {
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
            List<Boolean> values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void sleep(long milliseconds) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Delegate> getClickableChoices(List<String> choices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int choose(List<String> choices) {
        // TODO Auto-generated method stub
        return 0;
    }

}
