package teaselib.core.speechrecognition;

public class Word {

    public final String text;
    public final float probability;

    public Word(String text, float probability) {
        this.text = text;
        this.probability = probability;
    }

    @Override
    public String toString() {
        return "[" + text + " (" + probability + ")]";
    }

}