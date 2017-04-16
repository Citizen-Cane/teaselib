package teaselib.hosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import ss.IScript;
import teaselib.Actor;
import teaselib.Images;
import teaselib.core.Persistence;
import teaselib.core.UserItems;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.Stream;
import teaselib.util.TextVariables;

public class SexScriptsStatePersistence implements Persistence {

    private static final String DATA_PROPERTIES = "data.properties";
    private static final String DATA_PROPERTIES_BACKUP = "data backup.properties";
    private static final String PROPERTY_FILE_VALID_TAG = "intro.start_script";
    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private final ss.IScript host;
    private final PropertyNameMapping nameMapping = new SexScriptsPropertyNameMapping();
    private UserItems sexScriptsUserItems = new PreDefinedItems();

    public SexScriptsStatePersistence(IScript host) {
        this.host = host;
        if (has(PROPERTY_FILE_VALID_TAG)) {
            makeDataPropertiesBackup();
        }
    }

    private static void makeDataPropertiesBackup() {
        try {
            FileInputStream dataProperties = new FileInputStream(
                    new File(DATA_PROPERTIES));
            FileOutputStream dataPropertiesBackup = new FileOutputStream(
                    new File(DATA_PROPERTIES_BACKUP));
            try {
                Stream.copy(dataProperties, dataPropertiesBackup);
            } finally {
                dataProperties.close();
                dataPropertiesBackup.close();
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    @Override
    public PropertyNameMapping getNameMapping() {
        return nameMapping;
    }

    @Override
    public UserItems getUserItems() {
        return sexScriptsUserItems;
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
        name = getNameMapping().mapName(PropertyNameMapping.None,
                PropertyNameMapping.None, name);
        return host.loadString(name);
    }

    @Override
    public void set(String name, String value) {
        name = getNameMapping().mapName(PropertyNameMapping.None,
                PropertyNameMapping.None, name);
        host.save(name, value);
    }

    @Override
    public void set(String name, boolean value) {
        name = getNameMapping().mapName(PropertyNameMapping.None,
                PropertyNameMapping.None, name);
        set(name, value ? TRUE : FALSE);
    }

    @Override
    public void clear(String name) {
        name = getNameMapping().mapName(PropertyNameMapping.None,
                PropertyNameMapping.None, name);
        host.save(name, null);
    }

    @Override
    public TextVariables getTextVariables(Locale locale) {
        TextVariables variables = new TextVariables();
        variables.put(TextVariables.Names.Slave,
                getLocalized("intro.name", locale));
        return variables;
    }

    private String getLocalized(String name, Locale locale) {
        if (defaultLanguageMatches(locale)) {
            return get(name);
        } else {
            return get(name + "." + locale);
        }
    }

    private static boolean defaultLanguageMatches(Locale locale) {
        Locale defaultLocale = Locale.getDefault();
        return defaultLocale.getLanguage().equals(locale.getLanguage());
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
}
