package teaselib.core;

import java.io.File;
import java.io.IOException;

import teaselib.core.Host.Location;
import teaselib.core.texttospeech.TextToSpeechPlayer;
import teaselib.hosts.PreDefinedItems;

public final class TeaseLibConfigSetup implements Configuration.Setup {
    public static final String DEFAULTS = "defaults";

    private static final String TEASELIB_TEMPLATE = "teaselib.template";
    private static final String TEASELIB_PROPERTIES = "teaselib.properties";

    private static final String NETWORK_PROPERTIES = "network.properties";

    public static final String VOICES_PROPERTIES = "voices.properties";
    public static final String PRONUNCIATION_DIRECTORY = "pronunciation";

    private static final String ITEM_STORE_FILENAME = "items.xml";

    private final File teaseLibDefaultsPath;
    private final File userPath;

    public TeaseLibConfigSetup(Host host) {
        teaseLibDefaultsPath = new File(host.getLocation(Location.TeaseLib), DEFAULTS);
        userPath = host.getLocation(Location.User);
    }

    @Override
    public Configuration applyTo(Configuration config) throws IOException {
        userPath.mkdirs();

        addTeaseLibDefaults(config);
        addNetworkDefaults(config);
        addSpeechDefaults(config);

        return config;
    }

    private void addTeaseLibDefaults(Configuration config) throws IOException {
        config.addUserFile(new File(teaseLibDefaultsPath, TEASELIB_TEMPLATE), new File(userPath, TEASELIB_PROPERTIES));

        config.addConfigFile(new File(teaseLibDefaultsPath, TEASELIB_PROPERTIES));
        config.addConfigFile(new File(userPath, TEASELIB_PROPERTIES));

        config.addUserFile(PreDefinedItems.Settings.ITEM_STORE, new File(teaseLibDefaultsPath, ITEM_STORE_FILENAME),
                new File(userPath, ITEM_STORE_FILENAME));
    }

    private void addNetworkDefaults(Configuration config) throws IOException {
        config.addUserFile(new File(teaseLibDefaultsPath, NETWORK_PROPERTIES), new File(userPath, NETWORK_PROPERTIES));
        config.addConfigFile(new File(userPath, NETWORK_PROPERTIES));
    }

    private void addSpeechDefaults(Configuration config) throws IOException {
        config.addUserFile(TextToSpeechPlayer.Settings.Voices, new File(teaseLibDefaultsPath, VOICES_PROPERTIES),
                new File(userPath, VOICES_PROPERTIES));
        config.set(TextToSpeechPlayer.Settings.Pronunciation,
                new File(teaseLibDefaultsPath, PRONUNCIATION_DIRECTORY).getAbsolutePath());
    }
}
