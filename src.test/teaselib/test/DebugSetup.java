package teaselib.test;

import teaselib.Config;
import teaselib.core.Configuration;
import teaselib.core.Configuration.Setup;
import teaselib.core.devices.remote.LocalNetworkDevice;

public final class DebugSetup implements Setup {
    boolean stopOnRenderError = true;
    boolean remoteDeviceAccess = false;
    boolean enableOutput = false;
    boolean enableInput = false;

    @Override
    public void applyTo(Configuration config) {
        applyTeaseLibConfiguration(config);
        applyRemoteDeviceAccess(config);
        applyInput(config);
        applyOutput(config);
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

    protected void applyInput(Configuration config) {
        config.set(Config.InputMethod.SpeechRecognition, Boolean.toString(enableInput));
        config.set(Config.InputMethod.GameController, Boolean.toString(enableInput));
        config.set(Config.InputMethod.HeadGestures, Boolean.toString(enableInput));
    }

    protected void applyOutput(Configuration config) {
        config.set(Config.Render.Speech, Boolean.toString(enableOutput));
        config.set(Config.Render.Sound, Boolean.toString(enableOutput));
        config.set(Config.Render.ActorImages, Boolean.toString(enableOutput));
        config.set(Config.Render.InstructionalImages, Boolean.toString(enableOutput));
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

    public DebugSetup withInput() {
        enableInput = true;
        return this;
    }

    public DebugSetup withOutput() {
        enableOutput = true;
        return this;
    }
}
