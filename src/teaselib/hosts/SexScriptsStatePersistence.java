package teaselib.hosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ss.IScript;
import teaselib.Actor;
import teaselib.Images;
import teaselib.Sexuality.Gender;
import teaselib.core.Persistence;
import teaselib.core.TeaseLib;
import teaselib.core.UserItems;
import teaselib.core.UserItemsImpl;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.Stream;
import teaselib.util.TextVariables;

public class SexScriptsStatePersistence implements Persistence {
    private static final Logger logger = LoggerFactory.getLogger(SexScriptsHost.class);

    private static final String DATA_PROPERTIES = "data.properties";
    private static final String DATA_PROPERTIES_BACKUP = "data backup.properties";
    private static final String PROPERTY_FILE_VALID_TAG = "intro.start_script";
    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private final ss.IScript host;
    private final PropertyNameMapping nameMapping = new SexScriptsPropertyNameMapping();

    public SexScriptsStatePersistence(IScript host) {
        this.host = host;
        if (has(PROPERTY_FILE_VALID_TAG)) {
            makeDataPropertiesBackup();
        } else {
            if (new File(DATA_PROPERTIES_BACKUP).exists()) {
                throw new IllegalStateException("Data property file corrupt - restore it from backup.");
            } else {
                throw new IllegalStateException("Please run SexScripts intro first.");
            }
        }
    }

    private static void makeDataPropertiesBackup() {
        try (FileInputStream dataProperties = new FileInputStream(new File(DATA_PROPERTIES));
                FileOutputStream dataPropertiesBackup = new FileOutputStream(new File(DATA_PROPERTIES_BACKUP));) {
            Stream.copy(dataProperties, dataPropertiesBackup);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        return nameMapping;
    }

    @Override
    public UserItems getUserItems(TeaseLib teaseLib) throws IOException {
        // TODO Load SexScripts specific user items
        return new UserItemsImpl(teaseLib);
    }

    @Override
    public boolean has(String name) {
        return get(name) != null;
    }

    @Override
    public boolean getBoolean(String name) {
        String value = get(name);
        if (value == null) {
            return false;
        } else {
            return !value.equals(FALSE);
        }
    }

    @Override
    public String get(String name) {
        name = getNameMapping().mapName(PropertyNameMapping.None, PropertyNameMapping.None, name);
        return host.loadString(name);
    }

    @Override
    public void set(String name, String value) {
        name = getNameMapping().mapName(PropertyNameMapping.None, PropertyNameMapping.None, name);
        host.save(name, value);
    }

    @Override
    public void set(String name, boolean value) {
        name = getNameMapping().mapName(PropertyNameMapping.None, PropertyNameMapping.None, name);
        set(name, value ? TRUE : FALSE);
    }

    @Override
    public void clear(String name) {
        name = getNameMapping().mapName(PropertyNameMapping.None, PropertyNameMapping.None, name);
        host.save(name, null);
    }

    @Override
    public TextVariables getTextVariables(Locale locale) {
        TextVariables variables = new TextVariables();
        variables.put(TextVariables.Names.Slave, getLocalized("intro.name", locale));
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
        Locale defaultLocale = Locale.getDefault();
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
