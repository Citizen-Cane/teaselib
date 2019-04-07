package teaselib.core.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import teaselib.motiondetection.Gesture;

public class Choice {
    public final Gesture gesture;
    public final String text;
    public final String display;
    public final List<String> phrases;

    public static String getDisplay(Choice choice) {
        return choice.display;
    }

    public Choice(Gesture gesture, String choice, String display, List<String> phrases) {
        this.gesture = gesture;
        this.text = choice;
        this.display = display;
        this.phrases = phrases;
    }

    public Choice(Gesture gesture, String text, String display) {
        this(gesture, text, display, Collections.singletonList(display));
    }

    public Choice(String text, String display, String... phrases) {
        this(Gesture.None, text, display, Arrays.asList(phrases));
    }

    public Choice(String text, String display) {
        this(Gesture.None, text, display);
    }

    public Choice(String text) {
        this(text, text);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((display == null) ? 0 : display.hashCode());
        result = prime * result + ((gesture == null) ? 0 : gesture.hashCode());
        result = prime * result + ((phrases == null) ? 0 : phrases.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Choice other = (Choice) obj;
        if (display == null) {
            if (other.display != null)
                return false;
        } else if (!display.equals(other.display))
            return false;
        if (gesture != other.gesture)
            return false;
        if (phrases == null) {
            if (other.phrases != null)
                return false;
        } else if (!phrases.equals(other.phrases))
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Gesture=" + gesture + " text='" + text + "' display='" + display + "' phrases=" + phrases;
    }
}
