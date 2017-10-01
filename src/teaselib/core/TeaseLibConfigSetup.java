package teaselib.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import teaselib.core.Host.Location;
import teaselib.core.texttospeech.TextToSpeechPlayer;

final class TeaseLibConfigSetup implements Configuration.Setup {
    private static final String DEFAULTS = "defaults";

    private static final String TEASELIB_TEMPLATE = "teaselib.template";
    private static final String TEASELIB_PROPERTIES = "teaselib.properties";

    private static final String NETWORK_PROPERTIES = "network.properties";

    public static final String VOICES_PROPERTIES = "voices.properties";
    public static final String PRONOUNCIATION_DIRECTORY = "pronounciation";

    private final File teaseLibDefaultsPath;
    private final File userPath;

    TeaseLibConfigSetup(Host host) {
        teaseLibDefaultsPath = new File(host.getLocation(Location.TeaseLib), DEFAULTS);
        userPath = host.getLocation(Location.User);
    }

    @Override
    public void applyTo(Configuration config) throws IOException {
        userPath.mkdirs();

        addTeaseLibDefaults(config);
        addNetworkDefaults(config);
        addSpeechDefaults(config);
    }

    private void addTeaseLibDefaults(Configuration config) throws IOException, FileNotFoundException {
        config.addUserFile(new File(teaseLibDefaultsPath, TEASELIB_TEMPLATE), new File(userPath, TEASELIB_PROPERTIES));

        config.addConfigFile(new File(teaseLibDefaultsPath, TEASELIB_PROPERTIES));
        config.addConfigFile(new File(userPath, TEASELIB_PROPERTIES));

    }

    private void addNetworkDefaults(Configuration config) throws IOException {
        config.addUserFile(new File(teaseLibDefaultsPath, NETWORK_PROPERTIES), new File(userPath, NETWORK_PROPERTIES));
        config.addConfigFile(new File(userPath, NETWORK_PROPERTIES));
    }

    private void addSpeechDefaults(Configuration config) throws IOException {
        config.addUserFile(TextToSpeechPlayer.Settings.Voices, new File(teaseLibDefaultsPath, VOICES_PROPERTIES),
                new File(userPath, VOICES_PROPERTIES));
        config.set(TextToSpeechPlayer.Settings.Pronountiation,
                new File(teaseLibDefaultsPath, PRONOUNCIATION_DIRECTORY).getAbsolutePath());
    }
}
