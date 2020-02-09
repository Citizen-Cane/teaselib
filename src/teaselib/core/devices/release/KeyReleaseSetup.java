package teaselib.core.devices.release;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import teaselib.Actor;
import teaselib.Duration;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.State.Options;
import teaselib.State.Persistence.Until;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.Script;
import teaselib.core.ScriptEventArgs;
import teaselib.core.ScriptEvents;
import teaselib.core.ScriptEvents.ItemEventAction;
import teaselib.core.StateImpl;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceEvent;
import teaselib.core.devices.DeviceListener;
import teaselib.core.events.Event;
import teaselib.core.events.EventArgs;
import teaselib.core.events.EventSource;
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
    public final Map<Items, Consumer<Items>> acquireKeys = new LinkedHashMap<>();
    public final Map<Items, Consumer<Items>> acquireKeysAgain = new LinkedHashMap<>();
    public final Deque<Items> pendingPreparations = new ArrayDeque<>();

    private final Set<Actuator> actuators = new HashSet<>();
    public final Map<Items, Actuator> itemActuators = new HashMap<>();
    public final Map<Actuator, Items> actuatorItems = new HashMap<>();

    public final Map<Actuator, ItemEventAction> handledItems = new HashMap<>();
    public final Map<Actuator, ItemEventAction> installedCountDownActions = new HashMap<>();
    public final Map<Actuator, Event<ScriptEventArgs>> installedRenewHoldEvents = new HashMap<>();

    private final ScriptEvents events;

    public KeyReleaseSetup(TeaseScript script) {
        super(script, actor(script, Locale.ENGLISH));
        this.events = scriptRenderer.events;
    }

    public void init() {
        defaults();
        teaseLib.devices.get(KeyRelease.class).addDeviceListener(this);
    }

    private static Actor actor(Script script, Locale locale) {
        if (script.actor.locale().getLanguage().equalsIgnoreCase(locale.getLanguage())) {
            return script.actor;
        } else {
            Actor defaultDominant = script.teaseLib.getDominant(script.actor.gender, locale);
            defaultDominant.images = script.actor.images;
            return defaultDominant;
        }
    }

    private static String qualifiedName(Actuator actuator) {
        // TODO Implement white space handling in object persistence
        return DeviceCache.qualifiedName(actuator).replace(' ', '_');
    }

    // TODO restore actuators from multiple devices depending on handled domain and running actuator
    // - device may sleep, but handled items in KeyRelease domain are applied to the actuator name
    private void restoreHandledItems() {
        KeyRelease keyRelease = teaseLib.devices.get(KeyRelease.class).getDefaultDevice();
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
                singleRenewHoldEvent(actuator);
            }
            installReleaseAction(actuator, items, handled);
        }
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
                items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Detachable), //
                items(Toys.Collar, Toys.Humbler).matching(Features.Lockable) //
        );

        Items handcuffs = new Items( //
                items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).without(Features.Detachable), //
                items(Toys.Chains).matching(Features.Lockable) //
        );

        prepare(handcuffs, 1, TimeUnit.HOURS, defaultInstructions);
        prepare(cuffs, 2, TimeUnit.HOURS, defaultInstructions);
    }

    static class ItemsAlreadyPreparedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ItemsAlreadyPreparedException(Items items) {
            super("Items already prepared: " + items);
        }
    }

    public boolean deviceAvailable() {
        return !actuators.isEmpty();
    }

    public boolean devicesAvailable(Items... items) {
        // TODO Unassigned actuators or matching assignments - assignments can be replaced
        return actuators.size() >= items.length;
    }

    private void assign(Items items, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        Optional<Actuator> actuator = assigned(itemActuators, items);
        if (actuator.isPresent()) {
            unbind(actuator.get());
        }
        removeIntersecting(items);
        itemDurationSeconds.put(items, TimeUnit.SECONDS.convert(duration, unit));
        acquireKeys.put(items, instructions);
        if (instructionsAgain != null) {
            acquireKeysAgain.put(items, instructionsAgain);
        }
    }

    private void removeIntersecting(Items items) {
        remove(itemDurationSeconds, items);
        remove(pendingPreparations, items);
        remove(acquireKeys, items);
        remove(acquireKeysAgain, items);
    }

    private static void remove(Collection<Items> collection, Items items) {
        Set<Items> remove = new HashSet<>();
        collection.stream().filter(element -> !element.intersection(items).isEmpty()).forEach(remove::add);
        remove.stream().forEach(collection::remove);
    }

    private static <T> void remove(Map<Items, T> map, Items items) {
        Set<Items> remove = new HashSet<>();
        map.keySet().stream().filter(entry -> !entry.intersection(items).isEmpty()).forEach(remove::add);
        remove.stream().forEach(map::remove);
    }

    private Consumer<Items> defaultInstructions = items -> {
        // TODO perform interjection as prompt.script actor - not as creator of script
        // - interjections must use prompt.script.actor, not the one that created the script
        // -> until then, avoid speech & form of address
        show(items);
        showInterTitle("Place the keys on the hook!");
        reply("Done");
    };

    // TODO prefer lockable items as long as a key release device is available

    @Override
    public void deviceConnected(DeviceEvent<KeyRelease> e) {
        KeyRelease device = e.getDevice();
        device.actuators().stream().forEach(actuators::add);

        events.interjectScriptFragment(this::processPendingPreparations);
        restoreHandledItems();
    }

    void processPendingPreparations() {
        List<Items> unassigned = new ArrayList<>();
        for (Items items : pendingPreparations) {
            Optional<Actuator> actuator = chooseUnboundActuator(items);
            if (actuator.isPresent()) {
                bind(items, actuator.get());
                Consumer<Items> instructions = instructions(items);
                if (instructions != defaultInstructions) {
                    acquireKeys(items, actuator.get(), instructions);
                }
            } else {
                unassigned.add(items);
            }
        }
        pendingPreparations.clear();
        pendingPreparations.addAll(unassigned);
    }

    @Override
    public void deviceDisconnected(DeviceEvent<KeyRelease> e) {
        for (Actuator actuator : e.getDevice().actuators()) {
            unbind(actuator);
            actuators.remove(actuator);
        }
    }

    /**
     * Assign items to a key
     * 
     * @param items
     *            Items to be locked with the key
     * @param actuator
     *            Actuator to hold the key
     */
    private void bind(Items items, Actuator actuator) {
        unbind(actuator);

        itemActuators.put(items, actuator);
        actuatorItems.put(actuator, items);

        installApplyLock(actuator, items, instructions(items));
    }

    public boolean canPrepare(Item item) {
        return canPrepare(new Items(item));
    }

    /**
     * Whether the items can be prepared - actuator assigned, but keys aren't acquired yet for any item.
     * 
     * @param items
     * @return
     */
    public boolean canPrepare(Items items) {
        Optional<Actuator> assigned = assigned(itemActuators, items);
        if (assigned.isPresent()) {
            return !assigned.get().isRunning();
        } else {
            return true;
        }
    }

    public boolean isPrepared(Item item) {
        Optional<Actuator> actuator = assigned(itemActuators, new Items(item));
        return actuator.isPresent() && actuator.get().isRunning();
    }

    public boolean prepare(Item item, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return prepare(new Items(item), duration, unit, instructions, null);
    }

    public boolean prepare(Item item, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        return prepare(new Items(item), duration, unit, instructions, instructionsAgain);
    }

    public boolean isPrepared(Items items) {
        List<Actuator> assignedActuators = assignedActuators(items);
        return assignedActuators.stream().filter(Actuator::isRunning).count() == assignedActuators.size();
    }

    public boolean prepare(Items items, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return prepare(items, duration, unit, instructions, null);
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
    public boolean prepare(Items items, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        if (canPrepare(items)) {
            assign(items, duration, unit, instructions, instructionsAgain);
            Optional<Actuator> actuator = chooseUnboundActuator(items);
            if (actuator.isPresent()) {
                bind(items, actuator.get());
                acquireKeys(items, actuator.get(), instructions(items));
                return true;
            } else {
                pendingPreparations.addLast(items);
                return true;
            }
        } else {
            throw new ItemsAlreadyPreparedException(items);
        }

        // TODO Check overlaps - items should not appear in multiple assignments
    }

    private Optional<Actuator> chooseUnboundActuator(Items items) {
        Long duration = assigned(itemDurationSeconds, items).orElse(0l);
        List<Actuator> matching = Actuators.matching(actuators, duration, TimeUnit.SECONDS);
        return matching.stream().filter(Predicate.not(actuatorItems::containsKey)).findFirst();
    }

    private Consumer<Items> instructions(Items items) {
        return assigned(acquireKeys, items).orElseThrow();
    }

    private Optional<Consumer<Items>> instructionsAgain(Items items) {
        return assigned(acquireKeysAgain, items);
    }

    private static <T> Optional<T> assigned(Map<Items, T> map, Items items) {
        return map.entrySet().stream().filter(entry -> !entry.getKey().intersection(items).isEmpty())
                .map(Entry<Items, T>::getValue).findFirst();
    }

    private void acquireKeys(Items items, Actuator actuator, Consumer<Items> instructions) {
        actuator.arm();

        instructions.accept(items);

        singleRenewHoldEvent(actuator);
    }

    private List<Actuator> assignedActuators(Items items) {
        return handledItems.entrySet().stream().filter(entry -> !entry.getValue().items().intersection(items).isEmpty())
                .map(Entry<Actuator, ItemEventAction>::getKey).collect(Collectors.toList());
    }

    private void unbind(Actuator actuator) {
        removeEvents(actuator);
        ItemEventAction applyEventAction = handledItems.remove(actuator);
        if (applyEventAction != null) {
            events.itemApplied.remove(applyEventAction);
        }
        Items items = actuatorItems.remove(actuator);
        itemActuators.remove(items);
    }

    Optional<Actuator> getActuator(Item item) {
        return itemActuators.entrySet().stream().filter(entry -> entry.getKey().contains(item))
                .map(Entry<Items, Actuator>::getValue).findAny();
    }

    Optional<Actuator> getActuator(Items items) {
        return itemActuators.entrySet().stream().filter(entry -> entry.getKey().containsAll(items))
                .map(Entry<Items, Actuator>::getValue).findAny();
    }

    private void installApplyLock(Actuator actuator, Items items, Consumer<Items> instructions) {
        ItemEventAction action = events.when(items).applied().thenOnce(() -> {
            handledItems.remove(actuator);
            lock(actuator, items, instructions);
        });
        handledItems.put(actuator, action);
    }

    private void lock(Actuator actuator, Items items, Consumer<Items> instructions) {
        if (!actuator.isRunning()) {
            acquireKeys(items, actuator, instructions);
        }

        Items handled = items.of(domain(Gadgets.Key_Release));
        if (!handled.anyApplied()) {
            // TODO Duration should be updated during each hold event
            handled.applyTo(actuatorName(actuator)).over(actuator.available(TimeUnit.SECONDS), TimeUnit.SECONDS)
                    .remember(Until.Removed);
        }

        actuator.start();
        renewHold(actuator);
        singleRenewHoldEvent(actuator);
        startCountdownAction(actuator, items);
        installReleaseAction(actuator, items, handled);
    }

    private ItemEventAction startCountdownAction(Actuator actuator, Items items) {
        return installedCountDownActions.computeIfAbsent(actuator, key //
        -> events.when(items).duration().thenOnce(() -> {
            events.afterChoices.remove(singleRenewHoldEvent(actuator));
            startCountDown(actuator, items);
        }));
    }

    private Event<ScriptEventArgs> singleRenewHoldEvent(Actuator actuator) {
        return installedRenewHoldEvents.computeIfAbsent(actuator, key //
        -> events.afterChoices.add(eventArgs -> renewHold(actuator)));
    }

    private void installReleaseAction(Actuator actuator, Items items, Items handled) {
        events.when(items).removed().thenOnce(() -> {
            actuator.release();
            handled.removeFrom(actuatorName(actuator));
            removeEvents(actuator);

            Optional<Consumer<Items>> instructionsAgain = instructionsAgain(items);
            if (instructionsAgain.isPresent()) {
                acquireKeys(items, actuator, instructionsAgain.get());
            }
            installApplyLock(actuator, items, instructionsAgain.orElse(instructions(items)));
        });
    }

    private void removeEvents(Actuator actuator) {
        removeEvent(actuator, events.itemDuration, installedCountDownActions);
        removeEvent(actuator, events.afterChoices, installedRenewHoldEvents);
    }

    private static <K, S extends EventArgs, T extends Event<S>, E extends EventSource<S>> void removeEvent( //
            K key, E eventSource, Map<K, T> map) {
        T event = map.get(key);
        if (eventSource.contains(event)) {
            eventSource.remove(event);
            map.remove(key);
        }
    }

    private static String actuatorName(Actuator actuator) {
        return qualifiedName(actuator);
    }

    private static void startCountDown(Actuator actuator, Items items) {
        long seconds = items.stream().filter(item -> !item.expired()).map(Item::duration)
                .map(duration -> duration.remaining(TimeUnit.SECONDS)).reduce(Math::min).orElseThrow();
        actuator.start(seconds, TimeUnit.SECONDS);
    }

    private static void renewHold(Actuator actuator) {
        if (actuator.isRunning()) {
            actuator.hold();
        }
    }

}
