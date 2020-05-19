package teaselib.core.devices.release;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import teaselib.Actor;
import teaselib.Duration;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.State.Options;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.ScriptEventArgs;
import teaselib.core.ScriptEvents;
import teaselib.core.ScriptEvents.ItemEventAction;
import teaselib.core.ScriptInteractionImplementation;
import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceEvent;
import teaselib.core.devices.DeviceListener;
import teaselib.core.devices.release.KeyReleaseDeviceInteraction.Instructions;
import teaselib.core.events.Event;
import teaselib.core.events.EventArgs;
import teaselib.core.events.EventSource;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.QualifiedEnum;
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
public class KeyReleaseDeviceInteraction extends ScriptInteractionImplementation<Items, Instructions>
        implements DeviceListener<KeyRelease> {
    static class Instructions {
        public final Items items;
        public final long durationSeconds;
        public final Consumer<Items> acquireKeys;
        public final Optional<Consumer<Items>> acquireKeysAgain;

        public Instructions(Items items, long duration, TimeUnit unit, Consumer<Items> acquireKeys) {
            this(items, duration, unit, acquireKeys, Optional.empty());
        }

        public Instructions(Items items, long duration, TimeUnit unit, Consumer<Items> acquireKeys,
                Consumer<Items> acquireKeysAgain) {
            this(items, duration, unit, acquireKeys, Optional.ofNullable(acquireKeysAgain));
        }

        public Instructions(Items items, long duration, TimeUnit unit, Consumer<Items> acquireKeys,
                Optional<Consumer<Items>> acquireKeysAgain) {
            this.items = items;
            this.durationSeconds = TimeUnit.SECONDS.convert(duration, unit);
            this.acquireKeys = acquireKeys;
            this.acquireKeysAgain = acquireKeysAgain;
        }

    }

    private final Set<Actuator> actuators = new HashSet<>();
    private final Map<Items, Actuator> itemActuators = new HashMap<>();
    private final Map<Actuator, Items> actuatorItems = new HashMap<>();

    private final Map<Actuator, ItemEventAction> handledItems = new HashMap<>();
    private final Map<Actuator, ItemEventAction> installedCountDownActions = new HashMap<>();
    private final Map<Actuator, Event<ScriptEventArgs>> installedRenewHoldEvents = new HashMap<>();

    private final TeaseLib teaseLib;
    private final ScriptEvents events;

    private static BiPredicate<Items, Items> matchingItems = (a, b) -> !a.intersection(b).isEmpty();

    public KeyReleaseDeviceInteraction(TeaseLib teaseLib, ScriptEvents events) {
        super(matchingItems);
        this.teaseLib = teaseLib;
        this.events = events;
        teaseLib.devices.get(KeyRelease.class).addDeviceListener(this);
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
                .removeProxy(teaseLib.state(new QualifiedEnum(Gadgets.Key_Release).toString(), actuatorName));
        Items handled = new Items(actuatorState.peers().stream().filter(peer -> peer instanceof Item)
                .map(item -> (Item) item).collect(Collectors.toList()));

        if (!handled.equals(Items.None)) {
            Items items = teaseLib.relatedItems(TeaseLib.DefaultDomain, handled);
            Options appliedToItems = items.apply();
            Duration remaining = teaseLib.duration(actuator.remaining(TimeUnit.SECONDS), TimeUnit.SECONDS);

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
    public void setDefaults(Actor actor) {
        // TODO Decide whether to support different kinds of restraints,
        // or find out how to handle them without explicit queries
        // - force scripts to use matching(Features.Coupled/Features.Detachable)
        // - add anklets/wristlets as explicit type to Accessoires, however they're still connectable as
        // - find a way to specify that anklets/wristlets can be detached -> via removeFrom Hands/Wrists Tied
        // -> blocks applying/removing all states with applying/removing an item - but that feature isn't needed anyway
        Items cuffs = new Items( //
                teaseLib.items(TeaseLib.DefaultDomain, Toys.Wrist_Restraints, Toys.Ankle_Restraints)
                        .matching(Features.Detachable), //
                teaseLib.items(TeaseLib.DefaultDomain, Toys.Collar, Toys.Humbler).matching(Features.Lockable) //
        );

        Items handcuffs = new Items( //
                teaseLib.items(TeaseLib.DefaultDomain, Toys.Wrist_Restraints, Toys.Ankle_Restraints)
                        .without(Features.Detachable), //
                teaseLib.items(TeaseLib.DefaultDomain, Toys.Chains).matching(Features.Lockable) //
        );

        prepare(actor, handcuffs, 1, TimeUnit.HOURS, defaultInstructions);
        prepare(actor, cuffs, 2, TimeUnit.HOURS, defaultInstructions);
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

    private Instructions assign(Actor actor, Items items, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        Optional<Actuator> actuator = assigned(itemActuators, items);
        if (actuator.isPresent()) {
            unbind(actuator.get());
        }

        return definitions(actor).define(items,
                new Instructions(items, duration, unit, instructions, instructionsAgain));
    }

    private Consumer<Items> defaultInstructions = items -> {
        // No verbose default instruction - actuator is activated when applying items
    };

    // TODO prefer lockable items as long as a key release device is available

    @Override
    public void deviceConnected(DeviceEvent<KeyRelease> e) {
        KeyRelease device = e.getDevice();
        device.actuators().stream().forEach(actuators::add);

        events.interjectScriptFragment(this::processPendingPreparations);
        restoreHandledItems();
    }

    private void processPendingPreparations() {
        for (Actor actor : getActors()) {
            Definitions definitions = definitions(actor);
            Deque<Items> pendingPreparations = definitions.pending;

            List<Items> unassigned = new ArrayList<>();
            for (Items items : pendingPreparations) {
                Instructions definition = definitions.findMatching(items).orElseThrow();
                Optional<Actuator> actuator = chooseUnboundActuator(definition);
                if (actuator.isPresent()) {
                    bindAndAcquireKeys(actuator.get(), definition);
                } else {
                    unassigned.add(items);
                }
            }
            pendingPreparations.clear();
            pendingPreparations.addAll(unassigned);
        }
    }

    private boolean replaceWithPendingPreparation(Actor actor, Actuator actuator, Items removed) {
        Definitions definitions = definitions(actor);
        Deque<Items> pendingPreparations = definitions.pending;

        for (Items items : pendingPreparations) {
            if (removed.intersection(items).anyAvailable()) {
                unbind(actuator);
                bindAndAcquireKeys(actuator, definitions.findMatching(items).orElseThrow());
                pendingPreparations.remove(items);
                return true;
            }
        }
        return false;
    }

    private void bindAndAcquireKeys(Actuator actuator, Instructions definition) {
        bind(definition, actuator);
        // default instructions activate actuator at apply time
        if (definition.acquireKeys != defaultInstructions) {
            acquireKeys(actuator, definition);
        }
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
    private void bind(Instructions definition, Actuator actuator) {
        unbind(actuator);

        itemActuators.put(definition.items, actuator);
        actuatorItems.put(actuator, definition.items);

        installApplyLock(actuator, definition, false);
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

    public boolean prepare(Actor actor, Item item, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return prepare(actor, new Items(item), duration, unit, instructions, null);
    }

    public boolean prepare(Actor actor, Item item, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        return prepare(actor, new Items(item), duration, unit, instructions, instructionsAgain);
    }

    public boolean isPrepared(Items items) {
        List<Actuator> assignedActuators = assignedActuators(items);
        return assignedActuators.stream().filter(Actuator::isRunning).count() == assignedActuators.size();
    }

    public boolean prepare(Actor actor, Items items, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return prepare(actor, items, duration, unit, instructions, null);
    }

    public boolean prepare(Actor actor, Items items, Consumer<Items> instructions) {
        return prepare(actor, items, instructions, null);
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
    public boolean prepare(Actor actor, Items items, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        if (canPrepare(items)) {
            Instructions definition = assign(actor, items, duration, unit, instructions, instructionsAgain);
            Optional<Actuator> actuator = chooseUnboundActuator(definition);
            if (actuator.isPresent()) {
                bind(definition, actuator.get());
                acquireKeys(actuator.get(), definition);
                return true;
            } else {
                definitions(actor).pending.addLast(items);
                return true;
            }
        } else {
            throw new ItemsAlreadyPreparedException(items);
        }

        // TODO Check overlaps - items should not appear in multiple assignments
    }

    public boolean prepare(Actor actor, Items items, Consumer<Items> instructions, Consumer<Items> instructionsAgain) {
        Optional<Instructions> definition = definitions(actor).findMatching(items);
        long duration = definition.isPresent() ? definition.get().durationSeconds : 3600L;
        return prepare(actor, items, duration, TimeUnit.SECONDS, instructions, instructionsAgain);
    }

    private Optional<Actuator> chooseUnboundActuator(Instructions definition) {
        long duration = definition.durationSeconds;
        List<Actuator> matchingActuators = Actuators.matching(actuators, duration, TimeUnit.SECONDS);
        return matchingActuators.stream().filter(Predicate.not(actuatorItems::containsKey)).findFirst();
    }

    private static <T> Optional<T> assigned(Map<Items, T> map, Items items) {
        return map.entrySet().stream().filter(entry -> !entry.getKey().intersection(items).isEmpty())
                .map(Entry<Items, T>::getValue).findFirst();
    }

    private void acquireKeys(Actuator actuator, Instructions definition) {
        actuator.arm();
        definition.acquireKeys.accept(definition.items);
        singleRenewHoldEvent(actuator);
    }

    private void acquireKeysAgain(Actuator actuator, Instructions definition) {
        actuator.arm();

        Optional<Consumer<Items>> acquireKeysAgain = definition.acquireKeysAgain;
        if (acquireKeysAgain.isPresent()) {
            acquireKeysAgain.get().accept(definition.items);
        } else {
            definition.acquireKeys.accept(definition.items);
        }

        singleRenewHoldEvent(actuator);
    }

    private List<Actuator> assignedActuators(Items items) {
        return handledItems.entrySet().stream().filter(element -> matchingItems.test(element.getValue().items(), items))
                .map(Entry<Actuator, ItemEventAction>::getKey).collect(toList());
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

    private void installApplyLock(Actuator actuator, Instructions definition, boolean acquireAgain) {
        ItemEventAction action = events.when(definition.items).applied().thenOnce(() -> {
            handledItems.remove(actuator);
            lock(actuator, definition, acquireAgain);
        });
        handledItems.put(actuator, action);
    }

    private void lock(Actuator actuator, Instructions definition, boolean acquireAgain) {
        if (!actuator.isRunning()) {
            if (acquireAgain) {
                acquireKeysAgain(actuator, definition);
            } else {
                acquireKeys(actuator, definition);
            }
        }

        Items handled = teaseLib.relatedItems(Gadgets.Key_Release, definition.items);
        if (!handled.anyApplied()) {
            // TODO Duration should be updated during each hold event
            handled.applyTo(actuatorName(actuator)).over(actuator.available(TimeUnit.SECONDS), TimeUnit.SECONDS)
                    .remember(Until.Removed);
        }

        actuator.start();
        renewHold(actuator);
        singleRenewHoldEvent(actuator);
        startCountdownAction(actuator, definition.items);
        installReleaseAction(actuator, definition.items, handled, definition);
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
        installReleaseAction(actuator, items, handled, Optional.empty());
    }

    private void installReleaseAction(Actuator actuator, Items items, Items handled, Instructions definition) {
        installReleaseAction(actuator, items, handled, Optional.of(definition));
    }

    private void installReleaseAction(Actuator actuator, Items items, Items handled,
            Optional<Instructions> definition) {
        events.when(items).removed().thenOnce(() -> {
            actuator.release();
            handled.removeFrom(actuatorName(actuator));
            removeEvents(actuator);

            for (Actor actor : getActors()) {
                if (replaceWithPendingPreparation(actor, actuator, items)) {
                    break;
                }
            }

            if (definition.isPresent()) {
                Optional<Consumer<Items>> acquireKeysAgain = definition.get().acquireKeysAgain;
                if (acquireKeysAgain.isPresent()) {
                    acquireKeysAgain(actuator, definition.get());
                }
                installApplyLock(actuator, definition.get(), true);
            }
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
