package teaselib.test;

import teaselib.Config;
import teaselib.core.Configuration;
import teaselib.core.Configuration.Setup;
import teaselib.core.devices.remote.LocalNetworkDevice;

public final class DebugSetup implements Setup {
    @Override
    public void applyTo(Configuration config) {
        config.set(Config.Debug.StopOnAssetNotFound, Boolean.toString(true));
        config.set(Config.Debug.StopOnRenderError, Boolean.toString(true));
        config.set(Config.Debug.LogDetails, Boolean.toString(false));

        config.set(LocalNetworkDevice.Settings.EnableDeviceDiscovery, Boolean.toString(true));
        config.set(LocalNetworkDevice.Settings.EnableDeviceStatusListener, Boolean.toString(true));
    }

    public static final Configuration getConfiguration() {
        Configuration config = new Configuration();
        new DebugSetup().applyTo(config);
        return config;
    }
}