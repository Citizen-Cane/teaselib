package teaselib.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import teaselib.core.Host.Location;

final class TeaseLibConfigSetup implements Configuration.Setup {
    private static final String DEFAULTS = "defaults";

    private static final String TEASELIB_TEMPLATE = "teaselib.template";
    private static final String TEASELIB_PROPERTIES = "teaselib.properties";

    private static final String NETWORK_PROPERTIES = "network.properties";

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
}