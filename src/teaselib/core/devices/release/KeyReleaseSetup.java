package teaselib.core.devices.release;

import java.util.Locale;
import java.util.function.BiConsumer;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Mood;
import teaselib.ScriptFunction;
import teaselib.ScriptFunction.Relation;
import teaselib.TeaseScript;
import teaselib.core.Script;
import teaselib.core.ScriptEventArgs;
import teaselib.core.devices.DeviceCache;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.util.Item;
import teaselib.util.Items;

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

    public void setup(BiConsumer<KeyReleaseSetup, KeyRelease> handOverKeys) {
        boolean ready = false;
        while (!ready) {
            KeyRelease keyRelease = getKeyReleaseDevice();
            if (keyRelease.connected()) {
                showInterTitle("Device connected.");
            } else {
                append(Mood.Strict, "Please activate the key release device you want to use!");
                showInterTitle("Activating key release device!");
                Answer no = Answer.no("It doesn't work, #title");
                Answer deviceConnected = Answer.resume("Device connected, #title");
                Answer reply = reply(new ScriptFunction(() -> {
                    DeviceCache.connect(keyRelease);
                    return deviceConnected;
                }, Relation.Confirmation), deviceConnected, no);
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
                handOverKeys.accept(this, keyRelease);
                ready = keyRelease.connected();
            }
        }
    }

    private KeyRelease getKeyReleaseDevice() {
        return teaseLib.devices.get(KeyRelease.class).getDefaultDevice();
    }

    public void prepare(Actuator actuator, Item item) {
        prepare(actuator, new Items(item));
    }

    public void prepare(Actuator actuator, Items items) {
        actuator.arm();

        EventSource<ScriptEventArgs> afterChoices = events.afterChoices;
        Event<ScriptEventArgs> renewHold = new Event<ScriptEventArgs>() {
            @Override
            public void run(ScriptEventArgs eventArgs) throws Exception {
                if (actuator.isRunning()) {
                    actuator.hold();
                } else {
                    afterChoices.remove(this);
                }
            }
        };
        afterChoices.add(renewHold);

        events.when(items).applied().thenOnce(() -> afterChoices.remove(renewHold));
        events.when(items).applied().thenOnce(actuator::start);
        events.when(items).removed().thenOnce(actuator::release);
    }

}
