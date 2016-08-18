package teaselib.hosts;

import java.util.HashMap;
import java.util.Map;

import teaselib.Actor;
import teaselib.Images;
import teaselib.core.Persistence;
import teaselib.core.texttospeech.Voice;
import teaselib.util.TextVariables;

public class DummyPersistence implements Persistence {

    public final static String True = "true";
    public final static String False = "false";

    public final Map<String, String> storage = new HashMap<String, String>();

    @Override
    public boolean has(String name) {
        return storage.containsKey(name);
    }

    @Override
    public String get(String name) {
        return storage.get(name);
    }

    @Override
    public void set(String name, String value) {
        storage.put(name, value);
    }

    @Override
    public boolean getBoolean(String name) {
        String value = get(name);
        if (value == null) {
            return false;
        } else {
            return value.equals(True);
        }
    }

    @Override
    public void set(String name, boolean value) {
        set(name, value ? True : False);
    }

    @Override
    public void clear(String name) {
        storage.remove(name);
    }

    @Override
    public TextVariables getTextVariables(String locale) {
        return new TextVariables();
    }

    @Override
    public Actor getDominant(String locale) {
        return getDominant(Voice.Gender.Female, locale);
    }

    @Override
    public Actor getDominant(Voice.Gender gender, String locale) {
        switch (gender) {
        case Female:
            return new Actor("Mistress", "Miss", Voice.Gender.Female, locale,
                    Actor.Key.Dominant, Images.None);
        case Male:
            return new Actor("Master", "Sir", Voice.Gender.Male, locale,
                    Actor.Key.Dominant, Images.None);
        default:
            throw new IllegalArgumentException(gender.toString());
        }
    }
}
