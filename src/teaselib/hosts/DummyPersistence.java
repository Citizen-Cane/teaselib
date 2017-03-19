package teaselib.hosts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Images;
import teaselib.core.Persistence;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.PropertyNameMapping;
import teaselib.util.TextVariables;

public class DummyPersistence implements Persistence {
    private static final Logger logger = LoggerFactory
            .getLogger(DummyPersistence.class);

    public final static String True = "true";
    public final static String False = "false";

    public final Map<String, String> storage = new HashMap<String, String>();
    private final PropertyNameMapping nameMapping;

    public DummyPersistence() {
        nameMapping = new PropertyNameMapping();
    }

    public DummyPersistence(PropertyNameMapping propertyNameMapping) {
        nameMapping = propertyNameMapping;
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        return nameMapping;
    }

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
        if (value == null) {
            clear(name);
        } else {
            storage.put(name, value);
        }
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
    public TextVariables getTextVariables(Locale locale) {
        return new TextVariables();
    }

    @Override
    public Actor getDominant(Voice.Gender gender, Locale locale) {
        switch (gender) {
        case Female:
            return new Actor("Mistress", "Miss", Voice.Gender.Female, locale,
                    Actor.Key.DominantFemale, Images.None);
        case Male:
            return new Actor("Master", "Sir", Voice.Gender.Male, locale,
                    Actor.Key.DominantMale, Images.None);
        default:
            throw new IllegalArgumentException(gender.toString());
        }
    }

    public void printStorage() {
        List<Entry<String, String>> entryList = new ArrayList<Entry<String, String>>(
                storage.entrySet());
        Collections.sort(entryList, new Comparator<Entry<String, String>>() {
            @Override
            public int compare(Entry<String, String> o1,
                    Entry<String, String> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        logger.info("Storage: " + storage.size() + " entries");
        for (Entry<String, String> entry : entryList) {
            logger.info(entry.getKey() + "=" + entry.getValue());
        }
    }

    @Override
    public String toString() {
        return storage.toString();
    }
}
