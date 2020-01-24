package teaselib.core.devices.release;

import static java.util.concurrent.TimeUnit.SECONDS;
import static teaselib.Features.Detachable;
import static teaselib.Features.Lockable;
import static teaselib.Toys.Ankle_Restraints;
import static teaselib.Toys.Collar;
import static teaselib.Toys.Humbler;
import static teaselib.Toys.Wrist_Restraints;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import teaselib.Toys;
import teaselib.core.Script;
import teaselib.core.ScriptEventArgs;
import teaselib.core.ScriptEvents.ScriptEventAction;
import teaselib.core.StateImpl;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceEvent;
import teaselib.core.devices.DeviceListener;
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
public class KeyReleaseSetup extends TeaseScript implements DeviceListener<KeyRelease> {

    public final Map<Items, Long> itemDurationSeconds = new LinkedHashMap<>();
    public final Map<Items, Actuator> itemActuators = new HashMap<>();
    public final Map<Actuator, Items> actuatorItems = new HashMap<>();

    public final Map<Actuator, ScriptEventAction> handledItems = new HashMap<>();
    public final Map<Actuator, Event<ScriptEventArgs>> installedRenewHoldEvents = new HashMap<>();

    public KeyReleaseSetup(TeaseScript script) {
        super(script, getOrDefault(script, Locale.ENGLISH));
        defaults();
    }

    public void init() {
        defaults();
        restore();
    }

    public void setupOnDeviceConnect() {
        teaseLib.devices.get(KeyRelease.class).addDeviceListener(this);
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

    // TODO call this on startup
    // TODO call on connecting the device (which includes startup) from onDeviceConnect(Actuators)
    // TODO restore actuators from multiple devices depending on handled domain and running actuator
    // - device may sleep, but handled items in KeyRelease domain are applied to the actuator name
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

            Event<ScriptEventArgs> renewHoldEvent;
            if (!remaining.expired()) {
                appliedToItems.over(remaining);
                renewHoldEvent = null;
            } else {
                renewHoldEvent = installRenewHoldEvent(actuator);
            }
            installReleaseAction(actuator, items, handled, null, renewHoldEvent);
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

    /**
     * Sets default for items that can be locked.
     * <p>
     * All restraints are considered lockable since this is the main purpose of these devices.
     * <p>
     * Other potentially lockable items are requested explicitly.
     */
    public void defaults() {
        // TODO Decide whether to support different kinds of restraints,
        // or find out how to handle them without explicit queries
        // - force scripts to use matching(Features.Coupled/Features.Detachable)
        // - add anklets/wristlets as explicit type to Accessoires, however they're still connectable as
        // - find a way to specify that anklets/wristlets can be detached -> via removeFrom Hands/Wrists Tied
        // -> blocks applying/removing all states with applying/removing an item - but that feature isn't needed anyway
        Items cuffs = new Items( //
                items(Wrist_Restraints, Ankle_Restraints).matching(Detachable), //
                items(Collar, Humbler).matching(Lockable) //
        );

        Items handcuffs = new Items( //
                items(Wrist_Restraints, Ankle_Restraints).without(Detachable), //
                items(Toys.Chains).matching(Lockable) //
        );

        assign(handcuffs, 1, TimeUnit.HOURS);
        assign(cuffs, 2, TimeUnit.HOURS);
    }

    public void clearAssignments() {
        itemDurationSeconds.clear();
    }

    public void assign(Items items, long duration, TimeUnit unit) {
        itemDurationSeconds.put(items, TimeUnit.SECONDS.convert(duration, unit));
    }

    // TODO prefer lockable items as long as a key release device is available

    @Override
    public void deviceConnected(DeviceEvent<KeyRelease> e) {
        Actuators actuators = e.getDevice().actuators();
        for (Entry<Items, Long> entry : itemDurationSeconds.entrySet()) {
            Optional<Actuator> actuator = actuators.get(entry.getValue(), TimeUnit.SECONDS);
            if (actuator.isPresent()) {
                bind(actuator.get(), entry.getKey());
            }
        }
    }

    @Override
    public void deviceDisconnected(DeviceEvent<KeyRelease> e) {
        for (Actuator actuator : e.getDevice().actuators()) {
            clear(actuator);
        }
    }

    /**
     * Assign items to a key
     * 
     * @param actuator
     *            Actuator to hold the key
     * @param items
     *            Items to be locked with the key
     */
    private void bind(Actuator actuator, Items items) {
        clear(actuator);

        itemActuators.put(items, actuator);
        actuatorItems.put(actuator, items);

        installApplyLock(actuator, items);
    }

    public boolean canPrepare(Item item) {
        return canPrepare(new Items(item));
    }

    public boolean canPrepare(Items items) {
        List<Actuator> all = assignedActuators(items);
        return all.size() == 1;
    }

    public boolean prepare(Item item) {
        return prepare(new Items(item));
    }

    /**
     * Obtain the key and attach it to a previously assigned actuator.
     * <p>
     * The countdown will be started when {code items.apply().over(...)} is called. So to start the countdown after the
     * next prompt, use:
     * 
     * <pre>
     *     public void startCountdownAfterPrompt(Message instructions, Items restraints, Answer ready, Duration duration) {
     *         say(instructions);
     *         State.Options applied = items.apply();
     *         reply(ready);
     *         applied.over(duration);
     *     }
     * }
     * </pre>
     * 
     * @param items
     *            The items to be locked.
     */
    public boolean prepare(Items items) {
        List<Actuator> actuators = assignedActuators(items);
        if (actuators.size() > 1) {
            throw new IllegalArgumentException(
                    "Items assigned to multiple keys - use prefer() or matching() to narrow items");
        } else if (actuators.size() == 1) {
            prepare(items, actuators.get(0));
            return true;
        } else {
            return false;
        }
    }

    private void prepare(Items items, Actuator actuator) {
        // TODO show setup dialog if gadget is available or key release device running -> display setup instructions
        show(items);

        actuator.arm();
        Event<ScriptEventArgs> renewHoldEvent = installRenewHoldEvent(actuator);
        installedRenewHoldEvents.put(actuator, renewHoldEvent);
    }

    private List<Actuator> assignedActuators(Items items) {
        return handledItems.entrySet().stream().filter(entry -> !entry.getValue().items().intersection(items).isEmpty())
                .map(Entry<Actuator, ScriptEventAction>::getKey).collect(Collectors.toList());
    }

    private void clear(Actuator actuator) {
        Items items = actuatorItems.remove(actuator);
        itemActuators.remove(items);

        if (handledItems.containsKey(actuator)) {
            handledItems.remove(actuator);
        }
    }

    Optional<Actuator> getActuator(Item item) {
        return itemActuators.entrySet().stream().filter(entry -> entry.getKey().contains(item))
                .map(Entry<Items, Actuator>::getValue).findAny();
    }

    Optional<Actuator> getActuator(Items items) {
        return itemActuators.entrySet().stream().filter(entry -> entry.getKey().containsAll(items))
                .map(Entry<Items, Actuator>::getValue).findAny();
    }

    private void installApplyLock(Actuator actuator, Items items) {
        ScriptEventAction action = events.when(items).applied().thenOnce(() -> {
            handledItems.remove(actuator);
            lock(actuator, items);
        });
        handledItems.put(actuator, action);
    }

    private void lock(Actuator actuator, Items items) {
        if (!actuator.isRunning()) {
            // TODO display instructions when arming on apply
            actuator.arm();
        }

        Items handled = items.of(domain(Gadgets.Key_Release));
        if (!handled.anyApplied()) {
            actuator.start();
            renewHold(actuator);
            handled.applyTo(actuatorName(actuator)).remember();

            Event<ScriptEventArgs> renewHoldEvent = installedRenewHoldEvents.get(actuator);
            if (renewHoldEvent == null) {
                renewHoldEvent = installRenewHoldEvent(actuator);
            }
            ScriptEventAction startCountDownAction = installCountdownAction(actuator, items, renewHoldEvent);

            installReleaseAction(actuator, items, handled, startCountDownAction, renewHoldEvent);
        }
    }

    private ScriptEventAction installCountdownAction(Actuator actuator, Items items,
            Event<ScriptEventArgs> renewHoldEvent) {
        return events.when(items).duration().thenOnce(() -> {
            events.afterChoices.remove(renewHoldEvent);
            startCountDown(actuator, items);
        });
    }

    private Event<ScriptEventArgs> installRenewHoldEvent(Actuator actuator) {
        return events.afterChoices.add(eventArgs -> renewHold(actuator));
    }

    private void installReleaseAction(Actuator actuator, Items items, Items handled,
            ScriptEventAction startCountDownEvent, Event<ScriptEventArgs> renewHold) {
        events.when(items).removed().thenOnce(() -> {
            actuator.release();
            handled.removeFrom(actuatorName(actuator));

            if (events.itemDuration.contains(startCountDownEvent)) {
                events.itemDuration.remove(startCountDownEvent);
            }

            if (events.afterChoices.contains(renewHold)) {
                events.afterChoices.remove(renewHold);
                installedRenewHoldEvents.remove(actuator);
            }

            installApplyLock(actuator, items);
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
