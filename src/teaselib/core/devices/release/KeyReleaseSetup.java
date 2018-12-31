package teaselib.core.devices.release;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Mood;
import teaselib.ScriptFunction;
import teaselib.ScriptFunction.Relation;
import teaselib.TeaseScript;
import teaselib.core.Script;
import teaselib.core.devices.DeviceCache;

/**
 * Performs setup of the global stimulation controller.
 * 
 * @author Citizen-Cane
 *
 */
public class KeyReleaseSetup extends TeaseScript {

    public KeyReleaseSetup(TeaseScript script) {
        super(script, getOrDefault(script, Locale.ENGLISH));
    }

    private static Actor getOrDefault(Script script, Locale locale) {
        if (script.actor.locale().getLanguage().equalsIgnoreCase(locale.getLanguage())) {
            return script.actor;
        } else {
            Actor defaultDominant = script.teaseLib.getDominant(script.actor.gender, locale);
            defaultDominant.images = script.actor.images;
            return defaultDominant;
        }
    }

    public void setup(Consumer<KeyRelease> handOverKeys) {
        boolean ready = false;
        while (!ready) {
            KeyRelease keyRelease = getKeyReleaseDevice();
            if (keyRelease.connected()) {
                showInterTitle("Device connected.");
            } else {
                append(Mood.Strict, "Please activate the key release device you want to use!");
                showInterTitle("Activating key release device!");
                Answer no = Answer.no("It doesn't work, #title");
                String deviceConnected = "Device connected, #title";
                String reply = reply(new ScriptFunction(() -> {
                    DeviceCache.connect(keyRelease);
                    return deviceConnected;
                }, Relation.Confirmation), Answer.yes(deviceConnected), no);
                if (reply == deviceConnected) {
                    if (keyRelease.connected()) {
                        showInterTitle("Device connected.");
                    } else {
                        showInterTitle("Device not found - please check network connection.");
                        replace("Let's try turning it off and on again.");
                    }
                } else {
                    say("What a pity.");
                    break;
                }
            }

            if (keyRelease.connected()) {
                handOverKeys.accept(keyRelease);
                ready = keyRelease.connected();
            }
        }
    }

    private KeyRelease getKeyReleaseDevice() {
        return teaseLib.devices.get(KeyRelease.class).getDefaultDevice();
    }

    public void placeKey(Actuator actuator, long duration, TimeUnit unit, String answer, Object... items) {
        actuator.arm();
        state(actuator.releaseAction()).applyTo(items).over(duration, unit);
        agree(answer);
        actuator.start(duration, unit);
    }

}
