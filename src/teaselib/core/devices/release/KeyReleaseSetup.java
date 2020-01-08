package teaselib.core.devices.release;

import static java.util.concurrent.TimeUnit.*;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import teaselib.Actor;
import teaselib.Answer;
import teaselib.Duration;
import teaselib.Gadgets;
import teaselib.ScriptFunction;
import teaselib.ScriptFunction.Relation;
import teaselib.State.Options;
import teaselib.TeaseScript;
import teaselib.core.Script;
import teaselib.core.ScriptEventArgs;
import teaselib.core.ScriptEvents.ScriptEventAction;
import teaselib.core.StateImpl;
import teaselib.core.devices.DeviceCache;
import teaselib.core.events.Event;
import teaselib.core.state.AbstractProxy;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * Setup key release actuators
 * <li>arm & hold actuator
 * <li>start counting down on applying items
 * <li>release actuator on removing the items.
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
                    .allMatch(actuator -> actuator.isRunning() && state(actuatorName(actuator)).applied());
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
        String actuatorName = actuatorName(actuator);

        StateImpl actuatorState = (StateImpl) AbstractProxy
                .removeProxy(domain(Gadgets.Key_Release).state(actuatorName));
        Items handled = new Items(actuatorState.peers().stream().filter(peer -> peer instanceof Item)
                .map(item -> (Item) item).collect(Collectors.toList()));

        if (!handled.equals(Items.None)) {
            Items items = handled.of(defaultDomain);
            Options appliedToItems = items.apply();
            Duration remaining = duration(actuator.remaining(TimeUnit.SECONDS), TimeUnit.SECONDS);

            if (!remaining.expired()) {
                appliedToItems.over(remaining);
            } else {
                Event<ScriptEventArgs> renewHoldEvent = installRenewHoldEvent(actuator);
                events.when(items).removed().thenOnce(() -> events.afterChoices.remove(renewHoldEvent));
            }

            releaseOnRemove(actuator, items, handled);
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

    // TODO Add defaults for lockable items:
    // long actuator: detachable & lockable ankle & wrist cuffs, lockable collar/humbler/etc.
    // short actuator: chains, coupled hand/ankle cuffs, anything that restricts posture
    //
    // + Script may set duration for lockable items, or not
    // -> setup may override duration with Math.min(scriptDuration, actuator.remaining())
    // + script releases when appropriate, or
    // script awaits item().expired() (or remaining - x), to comment key release before the key drops.

    // TODO Remove use of actuator
    /**
     * 
     * Prepare the actuator for start/release when applying/removing the items.
     * 
     * @param actuator
     * @param items
     *            The items to link the actuator to. The holding duration is persisted in
     *            items.of(domain(Gadgets.Key_Release), and the items are applied to state(actuator.getName()).
     */
    public void prepare(Actuator actuator, Items items) {
        show(items);
        actuator.arm();

        Items handled = items.of(domain(Gadgets.Key_Release));
        handled.applyTo(actuatorName(actuator)).remember();

        events.when(items).applied().thenOnce(actuator::start);
        events.when(items).applied().thenOnce(() -> renewHold(actuator));
        events.when(items).applied().thenOnce(() -> handled.applyTo(actuatorName(actuator)));

        Event<ScriptEventArgs> renewHoldEvent = installRenewHoldEvent(actuator);
        ScriptEventAction startCountDownAction = installCountdownAction(actuator, items);
        ScriptEventAction removeRenewHoldAction = installRemoveHoldAction(items, renewHoldEvent);

        releaseOnRemove(actuator, items, handled);
        cleanupOnRemove(items, renewHoldEvent, startCountDownAction, removeRenewHoldAction);
    }

    private ScriptEventAction installRemoveHoldAction(Items items, Event<ScriptEventArgs> renewHoldEvent) {
        return events.when(items).duration().thenOnce(() -> events.afterChoices.remove(renewHoldEvent));
    }

    private ScriptEventAction installCountdownAction(Actuator actuator, Items items) {
        return events.when(items).duration().thenOnce(() -> startCountDown(actuator, items));
    }

    private Event<ScriptEventArgs> installRenewHoldEvent(Actuator actuator) {
        return events.afterChoices.add(eventArgs -> renewHold(actuator));
    }

    private void releaseOnRemove(Actuator actuator, Items items, Items handled) {
        events.when(items).removed().thenOnce(() -> {
            actuator.release();
            handled.removeFrom(actuatorName(actuator));
        });
    }

    private void cleanupOnRemove(Items items, Event<ScriptEventArgs> renewHold, ScriptEventAction startCountDownEvent,
            ScriptEventAction removeRenewHoldEvent) {
        events.when(items).removed().thenOnce(() -> {
            if (events.afterChoices.contains(renewHold)) {
                events.afterChoices.remove(renewHold);
            }
            if (events.itemDuration.contains(startCountDownEvent)) {
                events.itemDuration.remove(startCountDownEvent);
            }
            if (events.itemDuration.contains(removeRenewHoldEvent)) {
                events.itemDuration.remove(removeRenewHoldEvent);
            }
        });
    }

    private static String actuatorName(Actuator actuator) {
        return qualifiedName(actuator);
    }

    private static void startCountDown(Actuator actuator, Items items) {
        long seconds = items.stream().filter(item -> !item.expired()).map(Item::duration)
                .map(duration -> duration.remaining(SECONDS)).reduce(Math::min).orElseThrow();
        actuator.start(seconds, SECONDS);
    }

    private static void renewHold(Actuator actuator) {
        if (actuator.isRunning()) {
            actuator.hold();
        }
    }

}
