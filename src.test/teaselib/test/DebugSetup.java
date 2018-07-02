package teaselib.test;

import static org.junit.Assert.*;

import java.io.File;

import teaselib.Config;
import teaselib.core.AbstractUserItems;
import teaselib.core.Configuration;
import teaselib.core.Configuration.Setup;
import teaselib.core.TeaseLibConfigSetup;
import teaselib.core.devices.remote.LocalNetworkDevice;
import teaselib.core.speechrecognition.SpeechRecognitionResult.Confidence;
import teaselib.core.texttospeech.TextToSpeechPlayer;

public final class DebugSetup implements Setup {
    boolean stopOnRenderError = true;
    boolean enableRemoteDeviceAccess = false;
    boolean enableOutput = false;
    boolean enableInput = false;
    boolean enableDictionaries = false;
    // boolean loadItems = false;

    @Override
    public Configuration applyTo(Configuration config) {
        applyTeaseLibConfiguration(config);
        applyRemoteDeviceAccess(config);
        applyInput(config);
        applyOutput(config);
        applyDictionanries(config);
        loadDefaultItemStores(config);

        return config;
    }

    private void applyTeaseLibConfiguration(Configuration config) {
        config.set(Config.Debug.StopOnAssetNotFound, Boolean.toString(stopOnRenderError));
        config.set(Config.Debug.StopOnRenderError, Boolean.toString(stopOnRenderError));
        config.set(Config.Debug.LogDetails, Boolean.toString(false));
    }

    private void applyRemoteDeviceAccess(Configuration config) {
        config.set(LocalNetworkDevice.Settings.EnableDeviceDiscovery, Boolean.toString(enableRemoteDeviceAccess));
        config.set(LocalNetworkDevice.Settings.EnableDeviceStatusListener, Boolean.toString(enableRemoteDeviceAccess));
    }

    protected void applyInput(Configuration config) {
        config.set(Config.InputMethod.SpeechRecognition, Boolean.toString(enableInput));
        config.set(Config.InputMethod.GameController, Boolean.toString(enableInput));
        config.set(Config.InputMethod.HeadGestures, Boolean.toString(enableInput));

        config.set(Config.SpeechRecognition.Intention.Chat, Confidence.Low);
        config.set(Config.SpeechRecognition.Intention.Confirm, Confidence.Normal);
        config.set(Config.SpeechRecognition.Intention.Decide, Confidence.High);
    }

    protected void applyOutput(Configuration config) {
        config.set(Config.Render.Speech, Boolean.toString(enableOutput));
        config.set(Config.Render.Sound, Boolean.toString(enableOutput));
        config.set(Config.Render.ActorImages, Boolean.toString(enableOutput));
        config.set(Config.Render.InstructionalImages, Boolean.toString(enableOutput));

    }

    private void applyDictionanries(Configuration config) {
        if (enableDictionaries) {
            config.set(TextToSpeechPlayer.Settings.Pronunciation,
                    new File(TeaseLibConfigSetup.PRONUNCIATION_DIRECTORY).getAbsolutePath());
        }
    }

    private void loadDefaultItemStores(Configuration config) {
        // if (loadItems) {
        config.set(AbstractUserItems.Settings.ITEM_DEFAULT_STORE, new File("defaults/" + "items.xml").getPath());
        assertTrue(new File(config.get(AbstractUserItems.Settings.ITEM_DEFAULT_STORE)).exists());

        config.set(AbstractUserItems.Settings.ITEM_USER_STORE,
                new File("defaults/" + "useritems_template.xml").getPath());
        assertTrue(new File(config.get(AbstractUserItems.Settings.ITEM_USER_STORE)).exists());
        // }
    }

    public static Configuration getConfiguration() {
        Configuration config = new Configuration();
        new DebugSetup().applyTo(config);
        return config;
    }

    public static Configuration getConfigurationWithRemoteDeviceAccess() {
        Configuration config = new Configuration();
        new DebugSetup().withRemoteDeviceAccess().applyTo(config);
        return config;
    }

    public DebugSetup continueOnRenderErrors() {
        stopOnRenderError = false;
        return this;
    }

    public DebugSetup withRemoteDeviceAccess() {
        enableRemoteDeviceAccess = true;
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

    public DebugSetup withDictionaries() {
        enableDictionaries = true;
        return this;
    }

    // public DebugSetup withItems() {
    // loadItems = true;
    // return this;
    // }
}
