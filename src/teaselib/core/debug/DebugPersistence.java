package teaselib.core.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

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

    private final Function<Persistence, PropertyNameMapping> propertyMappingSupplier;

    public DebugPersistence() {
        this(new DebugStorage());
    }

    public DebugPersistence(Map<QualifiedName, String> storage) {
        this(storage, (persistence) -> new PropertyNameMapping(persistence));
    }

    public DebugPersistence(Function<Persistence, PropertyNameMapping> propertyMappingSupplier) {
        this(new DebugStorage(), propertyMappingSupplier);
    }

    public DebugPersistence(Map<QualifiedName, String> storage,
            Function<Persistence, PropertyNameMapping> propertyMappingSupplier) {
        this.storage = storage;
        this.propertyMappingSupplier = propertyMappingSupplier;
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        return propertyMappingSupplier.apply(this);
    }

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return new UserItemsImpl(teaseLib);
    }

    @Override
    public boolean has(QualifiedName name) {
        return storage.containsKey(name);
    }

    @Override
    public String get(QualifiedName name) {
        return storage.get(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        if (value == null) {
            clear(name);
        } else {
            storage.put(name, value);
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
