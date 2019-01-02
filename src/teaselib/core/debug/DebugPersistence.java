package teaselib.core.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Images;
import teaselib.Sexuality.Gender;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.UserItemsImpl;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.QualifiedName;
import teaselib.util.TextVariables;

public class DebugPersistence implements Persistence {
    private static final Logger logger = LoggerFactory.getLogger(DebugPersistence.class);

    public static final String True = "true";
    public static final String False = "false";

    public final Map<QualifiedName, String> storage;

    private final PropertyNameMapping nameMapping;

    public DebugPersistence() {
        this(new DebugStorage());
    }

    public DebugPersistence(Map<QualifiedName, String> storage) {
        this(storage, PropertyNameMapping.DEFAULT);
    }

    public DebugPersistence(PropertyNameMapping nameMapping) {
        this(new DebugStorage(), nameMapping);
    }

    public DebugPersistence(Map<QualifiedName, String> storage, PropertyNameMapping nameMapping) {
        this.storage = storage;
        this.nameMapping = nameMapping;
    }

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return new UserItemsImpl(teaseLib);
    }

    private QualifiedName mapped(QualifiedName name) {
        return nameMapping.map(name);
    }

    @Override
    public boolean has(QualifiedName name) {
        return storage.containsKey(mapped(name));
    }

    @Override
    public String get(QualifiedName name) {
        return nameMapping.get(name, () -> storage.get(mapped(name)));
    }

    @Override
    public void set(QualifiedName name, String value) {
        if (value == null) {
            clear(mapped(name));
        } else {
            nameMapping.set(name, value, (v) -> storage.put(mapped(name), v));
        }
    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        String value = get(name);
        if (value == null) {
            return false;
        } else {
            return value.equals(True);
        }
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        set(name, value ? True : False);
    }

    @Override
    public void clear(QualifiedName name) {
        storage.remove(mapped(name));
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
        List<Entry<QualifiedName, String>> entryList = new ArrayList<>(storage.entrySet());
        Collections.sort(entryList, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));
        if (logger.isInfoEnabled()) {
            logger.info("Storage: {} entries", storage.size());
            for (Entry<QualifiedName, String> entry : entryList) {
                logger.info("{}={}", entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public String toString() {
        return storage.toString();
    }
}
