package teaselib.core.util;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Images;
import teaselib.Sexuality.Gender;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.UserItemsImpl;
import teaselib.core.configuration.ConfigurationFile;
import teaselib.util.TextVariables;

public class CachedPersistenceImpl implements CachedPersistence {

    private static final Logger logger = LoggerFactory.getLogger(CachedPersistenceImpl.class);

    private final ConfigurationFile file;

    public CachedPersistenceImpl(ConfigurationFile file) {
        this.file = file;
    }

    @Override
    public ConfigurationFile file() {
        return file;
    }

    // TODO SexScripts reads/write case insensitive
    // -> replace SortedProperties to use String::compareToIgnoreCase
    // -> until then settings will not match because we're not going to write lowercase

    @Override
    public boolean has(QualifiedName name) {
        return has(name.toString());
    }

    private boolean has(String name) {
        return file.has(name);
    }

    @Override
    public String get(QualifiedName name) {
        return get(name.toString());
    }

    private String get(String name) {
        return file.get(name);
    }

    @Override
    public void set(QualifiedName name, String value) {
        set(name.toString(), value);

    }

    private void set(String name, String value) {
        file.set(name, value);

    }

    @Override
    public boolean getBoolean(QualifiedName name) {
        return getBoolean(name.toString());
    }

    private boolean getBoolean(String name) {
        return file.getBoolean(name);
    }

    @Override
    public void set(QualifiedName name, boolean value) {
        set(name.toString(), value);
    }

    private void set(String name, boolean value) {
        file.set(name, value);
    }

    @Override
    public void clear(QualifiedName name) {
        clear(name.toString());
    }

    private void clear(String name) {
        file.clear(name);
    }

    // Duplicated code from SexScriptsPersistence - refactor to host or TeaseLib

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        return new UserItemsImpl(teaseLib);
    }

    @Override
    public TextVariables getTextVariables(Locale locale) {
        var variables = new TextVariables();
        variables.set(TextVariables.Identity.Slave_Name, getLocalized("intro.name", locale));
        return variables;
    }

    private String getLocalized(String name, Locale locale) {
        String localizedVariableName = name + "." + locale.getLanguage();
        if (defaultLanguageMatches(locale)) {
            return get(name);
        } else if (has(localizedVariableName)) {
            return get(localizedVariableName);
        } else {
            String defaultValue = get(name);
            logger.warn("Localized name {} not found, using default '{}'", localizedVariableName, defaultValue);
            return defaultValue;
        }
    }

    private static boolean defaultLanguageMatches(Locale locale) {
        var defaultLocale = Locale.getDefault();
        return defaultLocale.getLanguage().equals(locale.getLanguage());
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

}
