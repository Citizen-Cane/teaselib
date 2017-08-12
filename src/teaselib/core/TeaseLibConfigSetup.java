package teaselib.core;

import java.io.File;
import java.io.IOException;

import teaselib.core.Host.Location;

final class TeaseLibConfigSetup implements Configuration.Setup {
    private final Host host;

    TeaseLibConfigSetup(Host host) {
        this.host = host;
    }

    @Override
    public void applyTo(Configuration config) throws IOException {
        config.addConfigFile(new File(host.getLocation(Location.TeaseLib), "defaults/teaselib.properties"));
        config.addUserFile(new File(host.getLocation(Location.TeaseLib), "defaults/teaselib.template"),
                new File(host.getLocation(Location.Host), "teaselib.properties"));
        config.addConfigFile(new File(host.getLocation(Location.Host), "teaselib.properties"));
    }
}