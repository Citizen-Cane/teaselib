package teaselib.core.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Images;
import teaselib.Sexuality.Gender;
import teaselib.core.UserItemsImpl;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.util.PropertyNameMapping;
import teaselib.util.TextVariables;

public class DebugPersistence implements Persistence {
    private static final Logger logger = LoggerFactory.getLogger(DebugPersistence.class);

    public static final String True = "true";
    public static final String False = "false";

    public static final class Storage extends HashMap<String, String> {
        private static final long serialVersionUID = 1L;
    }

    public final Map<String, String> storage;

    private final PropertyNameMapping nameMapping;

    public DebugPersistence() {
        this(new PropertyNameMapping());
    }

    public DebugPersistence(Map<String, String> storage) {
        this(storage, new PropertyNameMapping());
    }

    public DebugPersistence(PropertyNameMapping propertyNameMapping) {
        this(new Storage(), propertyNameMapping);
    }

    public DebugPersistence(Map<String, String> storage, PropertyNameMapping propertyNameMapping) {
        this.storage = storage;
        this.nameMapping = propertyNameMapping;
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        return nameMapping;
    }

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return new UserItemsImpl(teaseLib);
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
    public Actor getDominant(Gender gender, Locale locale) {
        switch (gender) {
        case Feminine:
            return new Actor("Mistress", "Miss", gender, locale, Actor.Key.DominantFemale, Images.None);
        case Masculine:
            return new Actor("Master", "Sir", gender, locale, Actor.Key.DominantMale, Images.None);
        default:
            throw new IllegalArgumentException(gender.toString());
        }
    }

    public void printStorage() {
        List<Entry<String, String>> entryList = new ArrayList<>(storage.entrySet());
        Collections.sort(entryList, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
        if (logger.isInfoEnabled()) {
            logger.info("Storage: " + storage.size() + " entries");
            for (Entry<String, String> entry : entryList) {
                logger.info(entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return storage.toString();
    }
}
