package teaselib.core.devices.release;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Gadgets;
import teaselib.ScriptFunction;
import teaselib.ScriptFunction.Relation;
import teaselib.TeaseScript;
import teaselib.core.Script;
import teaselib.core.ScriptEventArgs;
import teaselib.core.StateImpl;
import teaselib.core.devices.DeviceCache;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.core.state.AbstractProxy;
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

    public boolean isConnected() {
        KeyRelease keyRelease = getKeyReleaseDevice();
        return keyRelease.connected();
    }

    public boolean isPrepared() {
        KeyRelease keyRelease = getKeyReleaseDevice();
        if (keyRelease.connected()) {
            restore(keyRelease);

            return keyRelease.actuators().stream()
                    .allMatch(actuator -> actuator.isRunning() && state(qualifiedName(actuator)).applied());
        } else {
            return false;
        }
    }

    private static String qualifiedName(Actuator actuator) {
        // TODO Implement white space handling in object persistence
        return DeviceCache.qualifiedName(actuator).replace(' ', '_');
    }

    public void restore() {
        KeyRelease keyRelease = getKeyReleaseDevice();
        if (keyRelease.active()) {
            restore(keyRelease);
        }
    }

    private void restore(KeyRelease keyRelease) {
        keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(this::restore);
    }

    private void restore(Actuator actuator) {
        String actuatorName = qualifiedName(actuator);

        StateImpl actuatorState = (StateImpl) AbstractProxy
                .removeProxy(domain(Gadgets.Key_Release).state(actuatorName));
        Items handled = new Items(actuatorState.peers().stream().filter(peer -> peer instanceof Item)
                .map(item -> (Item) item).collect(Collectors.toList()));

        if (!handled.equals(Items.None)) {
            handled.applyTo(actuatorName).over(actuator.remaining(TimeUnit.SECONDS), TimeUnit.SECONDS);

            Items items = handled.of(defaultDomain);
            items.apply();

            events.when(items).removed().thenOnce(actuator::release);
            events.when(items).removed().thenOnce(() -> handled.removeFrom(actuatorName));
        }
    }

    public boolean setup(BiConsumer<KeyReleaseSetup, KeyRelease> handOverKeys) {
        boolean ready = false;
        while (!ready) {
            KeyRelease keyRelease = getKeyReleaseDevice();
            if (keyRelease.connected()) {
                // TODO show in ui as notification
                // showInterTitle("Device connected.");
            } else {
                showInterTitle("Activate key release device!");
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
                        showInterTitle("Device not found - please reset device and check network connection.");
                    }
                } else {
                    break;
                }
            }

            if (keyRelease.connected()) {
                handOverKeys.accept(this, keyRelease);
                ready = keyRelease.connected();
            }
        }

        return ready;
    }

    private KeyRelease getKeyReleaseDevice() {
        return teaseLib.devices.get(KeyRelease.class).getDefaultDevice();
    }

    public void prepare(Actuator actuator, Item item) {
        prepare(actuator, new Items(item));
    }

    // TODO Remove use of actuator
    /**
     * 
     * Prepare the actuator for start/release when applying/removing the items.
     * 
     * @param actuator
     * @param items
     *            The items to link the actuator to. The holding duration is persisted in
     *            items.of(domain(Gadgets.Key_Release), and the items are applied to state(actuator.getNaem()).
     */
    public void prepare(Actuator actuator, Items items) {
        show(items);
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

        Items handled = items.of(domain(Gadgets.Key_Release));
        handled.applyTo(qualifiedName(actuator));

        String actuatorName = qualifiedName(actuator);

        events.when(items).applied().thenOnce(() -> afterChoices.remove(renewHold));
        events.when(items).applied().thenOnce(actuator::start);
        events.when(items).applied().thenOnce(
                () -> handled.applyTo(actuatorName).over(actuator.remaining(TimeUnit.SECONDS), TimeUnit.SECONDS));

        events.when(items).removed().thenOnce(actuator::release);
        events.when(items).removed().thenOnce(() -> handled.removeFrom(actuatorName));
    }

}
