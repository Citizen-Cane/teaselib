package teaselib.core.ui;

import java.util.ArrayList;
import java.util.Arrays;

import teaselib.motiondetection.Gesture;

public class Choice extends ArrayList<String> {
    private static final long serialVersionUID = 1L;

    public final Gesture gesture;
    public final String text;

    public static String Display(Choice choice) {
        return choice.get(0);
    }

    public Choice(Gesture gesture, String choice, String... display) {
        this.gesture = gesture;
        this.text = choice;
        this.addAll(Arrays.asList(display));
    }

    public Choice(String text, String... display) {
        this(Gesture.None, text, display);
    }

    public Choice(String text, String display) {
        this(Gesture.None, text, display);
    }

    public Choice(String text) {
        this(text, text);
    }

    @Override
    public String toString() {
        return "Gesture=" + gesture + " text='" + text + "' display='" + this.get(0) + "...'";
    }
}
