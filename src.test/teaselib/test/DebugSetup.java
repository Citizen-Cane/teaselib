package teaselib.test;

import teaselib.Config;
import teaselib.core.Configuration;
import teaselib.core.Configuration.Setup;
import teaselib.core.devices.remote.LocalNetworkDevice;

public final class DebugSetup implements Setup {
    boolean stopOnRenderError = true;
    boolean remoteDeviceAccess = false;

    @Override
    public void applyTo(Configuration config) {
        applyTeaseLibConfiguration(config);
        applyRemoteDeviceAccess(config);
    }

    private void applyTeaseLibConfiguration(Configuration config) {
        config.set(Config.Debug.StopOnAssetNotFound, Boolean.toString(stopOnRenderError));
        config.set(Config.Debug.StopOnRenderError, Boolean.toString(stopOnRenderError));
        config.set(Config.Debug.LogDetails, Boolean.toString(false));
    }

    private void applyRemoteDeviceAccess(Configuration config) {
        config.set(LocalNetworkDevice.Settings.EnableDeviceDiscovery, Boolean.toString(remoteDeviceAccess));
        config.set(LocalNetworkDevice.Settings.EnableDeviceStatusListener, Boolean.toString(remoteDeviceAccess));
    }

    public static final Configuration getConfiguration() {
        Configuration config = new Configuration();
        new DebugSetup().applyTo(config);
        return config;
    }

    public static final Configuration getConfigurationWithRemoteDeviceAccess() {
        Configuration config = new Configuration();
        new DebugSetup().withRemoteDeviceAccess().applyTo(config);
        return config;
    }

    public DebugSetup continueOnRenderErrors() {
        stopOnRenderError = false;
        return this;
    }

    public DebugSetup withRemoteDeviceAccess() {
        remoteDeviceAccess = true;
        return this;
    }
}
