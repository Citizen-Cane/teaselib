package teaselib.core.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import teaselib.core.UserItemsImpl;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.host.Host;
import teaselib.host.Host.Location;

/**
 * @author Citizen-Cane
 *
 */
public final class TeaseLibConfigSetup implements Setup {
    private static final String TEASELIB_PROPERTIES = "teaselib.properties";
    private static final String NETWORK_PROPERTIES = "network.properties";
    private static final String VOICES_PROPERTIES = "voices.properties";

    private static final String ITEM_TEMPLATE_STORE_FILENAME = "useritems_template.xml";
    private static final String ITEM_USER_STORE_FILENAME = "useritems.xml";

    private final File teaseLibPath;
    private final File userPath;
    private final File scriptItemsPath;
    private final File userItemsPath;

    public TeaseLibConfigSetup(Host host) {
        teaseLibPath = host.getLocation(Location.TeaseLib);
        userPath = host.getLocation(Location.User);
        userItemsPath = new File(host.getLocation(Location.User), "User Items");
        scriptItemsPath = new File(host.getLocation(Location.User), "Script Items");
    }

    @Override
    public Configuration applyTo(Configuration config) throws IOException {
        userPath.mkdirs();

        addTeaseLibDefaults(config);
        addNetworkDefaults(config);
        addSpeechDefaults(config);
        addIdentitiesDefaults(config);

        config.setUserPath(Optional.of(userPath));
        return config;
    }

    private void addTeaseLibDefaults(Configuration config) throws IOException {
        config.add(Configuration.DEFAULTS, TEASELIB_PROPERTIES, userPath);

        config.set(UserItemsImpl.Settings.USER_ITEMS_PATH, userItemsPath.getAbsolutePath());
        config.set(UserItemsImpl.Settings.SCRIPT_ITEMS_PATH, scriptItemsPath.getAbsolutePath());

        config.set(UserItemsImpl.Settings.ITEM_DEFAULT_STORE, Configuration.DEFAULTS + ITEM_DEFAULT_STORE_FILENAME);
        userItemsPath.mkdirs();
        config.addUserFile(UserItemsImpl.Settings.ITEM_USER_STORE,
                Configuration.DEFAULTS + ITEM_TEMPLATE_STORE_FILENAME,
                new File(userItemsPath, ITEM_USER_STORE_FILENAME));
    }

    private void addNetworkDefaults(Configuration config) throws IOException {
        config.add(Configuration.DEFAULTS, NETWORK_PROPERTIES, userPath);
    }

    private void addSpeechDefaults(Configuration config) throws IOException {
        config.addUserFile(TextToSpeechPlayer.Settings.Voices, Configuration.DEFAULTS + VOICES_PROPERTIES,
                new File(userPath, VOICES_PROPERTIES));
        config.set(TextToSpeechPlayer.Settings.Pronunciation,
                new File(teaseLibPath, PRONUNCIATION_DIRECTORY).getAbsolutePath());
    }

    private void addIdentitiesDefaults(Configuration config) throws IOException {
        config.addPersistentUserProperties(Configuration.DEFAULTS, IDENTITY_PROPERTIES, userPath,
                IDENTITY_PROPERTIES_NAMESPACES);
    }

}
