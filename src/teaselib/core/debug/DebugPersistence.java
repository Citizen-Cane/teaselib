package teaselib.core.debug;

import java.io.IOException;
import java.util.Locale;

import teaselib.Actor;
import teaselib.Images;
import teaselib.Sexuality.Gender;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.UserItemsImpl;
import teaselib.core.util.QualifiedName;
import teaselib.util.TextVariables;

public class DebugPersistence implements Persistence {
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public final DebugStorage storage;

    public DebugPersistence() {
        this(new DebugStorage());
    }

    public DebugPersistence(DebugStorage storage) {
        this.storage = storage;
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
            return value.equals(TRUE);
        }
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        set(name, value ? TRUE : FALSE);
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

    @Override
    public String toString() {
        return storage.toString();
    }
}
