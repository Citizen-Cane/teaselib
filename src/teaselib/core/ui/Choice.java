package teaselib.core.ui;

import teaselib.motiondetection.Gesture;

public class Choice {

    public final Gesture gesture;
    public final String text;
    public final String display;

    public Choice(Gesture gesture, String choice, String display) {
        this.gesture = gesture;
        this.text = choice;
        this.display = display;
    }

    public Choice(String text, String display) {
        this(Gesture.None, text, display);
    }
}
