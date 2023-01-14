package teaselib.core.devices.release;

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
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import teaselib.Actor;
import teaselib.Bondage;
import teaselib.Duration;
import teaselib.Features;
import teaselib.Gadgets;
import teaselib.State.Options;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.DeviceInteractionDefinitions;
import teaselib.core.DeviceInteractionImplementation;
import teaselib.core.ItemsImpl;
import teaselib.core.ScriptEventArgs;
import teaselib.core.ScriptEvents;
import teaselib.core.ScriptEvents.ItemEventAction;
import teaselib.core.ScriptRenderer;
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
import teaselib.core.util.QualifiedString;
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
public class KeyReleaseDeviceInteraction extends DeviceInteractionImplementation<Items, Instructions>
        implements DeviceListener<KeyRelease> {

    static class Instructions {
        public final Actor actor;
        public final Items items;
        public final long durationSeconds;
        public final Consumer<Items> acquireKeys;
        public final Optional<Consumer<Items>> acquireKeysAgain;

        Instructions(Actor actor, Items items, long duration, TimeUnit unit, Consumer<Items> acquireKeys) {
            this(actor, items, duration, unit, acquireKeys, Optional.empty());
        }

        Instructions(Actor actor, Items items, long duration, TimeUnit unit, Consumer<Items> acquireKeys,
                Consumer<Items> acquireKeysAgain) {
            this(actor, items, duration, unit, acquireKeys, Optional.ofNullable(acquireKeysAgain));
        }

        Instructions(Actor actor, Items items, long duration, TimeUnit unit, Consumer<Items> acquireKeys,
                Optional<Consumer<Items>> acquireKeysAgain) {
            this.actor = actor;
            this.items = items;
            this.durationSeconds = TimeUnit.SECONDS.convert(duration, unit);
            this.acquireKeys = acquireKeys;
            this.acquireKeysAgain = acquireKeysAgain;
        }

    }

    private final Set<Actuator> actuators = new HashSet<>();
    private final Map<Items, Actuator> itemActuators = new HashMap<>();
    private final Map<String, Items> actuatorItems = new HashMap<>();

    private final Map<String, ItemEventAction> handledItems = new HashMap<>();
    private final Map<String, ItemEventAction> installedCountDownActions = new HashMap<>();
    private final Map<String, Event<ScriptEventArgs>> installedRenewHoldEvents = new HashMap<>();

    private final TeaseLib teaseLib;
    private final ScriptRenderer scriptRenderer;
    private final ScriptEvents events;

    private static BiPredicate<Items, Items> matchingItems = (a, b) -> !a.intersection(b).isEmpty();

    public KeyReleaseDeviceInteraction(TeaseLib teaseLib, ScriptRenderer scriptRenderer) {
        super(matchingItems);
        this.teaseLib = teaseLib;
        this.scriptRenderer = scriptRenderer;
        this.events = scriptRenderer.events;
        teaseLib.devices.get(KeyRelease.class).addDeviceListener(this);
    }

    private static String actuatorName(Actuator actuator) {
        // TODO Implement white space handling in object persistence
        return DeviceCache.qualifiedName(actuator).replace(' ', '_');
    }

    private static String actuatorKey(Actuator actuator) {
        return actuator.getDevicePath();
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
        StateImpl actuatorState = AbstractProxy.removeProxy(
                teaseLib.state(QualifiedString.of(Gadgets.Key_Release).toString(), actuatorName(actuator)));
        Items handled = new ItemsImpl(
                actuatorState.peers().stream().filter(peer -> peer instanceof Item).map(item -> (Item) item).toList());

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

        var cuffs = teaseLib.items(
                teaseLib.items(TeaseLib.DefaultDomain, Bondage.Wrist_Restraints, Bondage.Ankle_Restraints)
                        .matching(Features.Lockable).matching(Features.Detachable),
                teaseLib.items(TeaseLib.DefaultDomain, Toys.Collar, Toys.Humbler)
                        .matching(Features.Lockable));

        var handcuffs = teaseLib.items(
                teaseLib.items(TeaseLib.DefaultDomain, Bondage.Wrist_Restraints, Bondage.Ankle_Restraints)
                        .matching(Features.Lockable).without(Features.Detachable),
                teaseLib.items(TeaseLib.DefaultDomain, Bondage.Chains));

        prepare(actor, handcuffs, 1, TimeUnit.HOURS, DefaultInstructions);
        prepare(actor, cuffs, 2, TimeUnit.HOURS, DefaultInstructions);
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
                new Instructions(actor, items, duration, unit, instructions, instructionsAgain));
    }

    static final Consumer<Items> DefaultInstructions = items -> { /**/ };

    // TODO prefer lockable items as long as a key release device is available

    @Override
    public void deviceConnected(DeviceEvent<KeyRelease> e) {
        KeyRelease device = e.getDevice();
        device.actuators().stream().forEach(actuators::add);

        events.interjectScriptFragment(() -> {
            processPendingPreparations();
            processLateApply();
        });

        restoreHandledItems();
    }

    private void processPendingPreparations() {
        Actor actor = currentActor();
        DeviceInteractionDefinitions<Items, Instructions> definitions = definitions(actor);
        Deque<Items> pendingPreparations = definitions.pending;

        List<Items> unassigned = new ArrayList<>();
        for (Items items : pendingPreparations) {
            Instructions definition = definitions.findMatching(items).orElseThrow();
            Optional<Actuator> actuator = chooseUnboundActuator(definition);
            if (actuator.isPresent() && definition.items.noneApplied()) {
                bind(actuator.get(), definition);
                if (!isDefault(definition)) {
                    acquireKeys(actuator.get(), definition);
                }
            } else {
                unassigned.add(items);
            }
        }
        pendingPreparations.clear();
        pendingPreparations.addAll(unassigned);
    }

    private void processLateApply() {
        Actor actor = currentActor();
        DeviceInteractionDefinitions<Items, Instructions> definitions = definitions(actor);

        Predicate<Entry<Items, Instructions>> unbound = e -> getActuator(e.getKey()).isEmpty();
        BinaryOperator<Entry<Items, Instructions>> maxApplied = (a,
                b) -> a.getKey().getApplied().size() >= b.getKey().getApplied().size() ? a : b;

        Optional<Instructions> definition = definitions.stream().filter(unbound).reduce(maxApplied)
                .map(Entry::getValue);

        if (definition.isPresent()) {
            Optional<Actuator> actuator = chooseUnboundActuator(definition.get());
            if (actuator.isPresent()) {
                bindActuatorAfterApply(definition.get(), actuator.get());
            }
        }
    }

    private void bindActuatorAfterApply(Instructions definition, Actuator actuator) {
        bind(actuator, definition);

        events.itemApplied.remove(handledItems.get(actuatorKey(actuator)));
        handledItems.remove(actuatorKey(actuator));

        acquireKeysIfNecessary(actuator, definition, false);
        installAfterApplyLock(actuator, definition);
    }

    private Actor currentActor() {
        return scriptRenderer.currentActor();
    }

    private void passKeys(Actor actor, Actuator actuator, Items removed) {
        DeviceInteractionDefinitions<Items, Instructions> definitions = definitions(actor);
        Deque<Items> pendingPreparations = definitions.pending;

        for (Items items : pendingPreparations) {
            if (removed.intersection(items).anyAvailable()) {
                unbind(actuator);
                Instructions definition = definitions.findMatching(items).orElseThrow();
                bind(actuator, definition);
                if (!isDefault(definition)) {
                    acquireKeys(actuator, definition);
                }
                pendingPreparations.remove(items);
            }
        }
    }

    private static boolean isDefault(Instructions definition) {
        return definition.acquireKeys == DefaultInstructions;
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
     * @param actuator
     *            Actuator to hold the key
     * @param items
     *            Items to be locked with the key
     */
    private void bind(Actuator actuator, Instructions definition) {
        unbind(actuator);

        itemActuators.put(definition.items, actuator);
        actuatorItems.put(actuatorKey(actuator), definition.items);

        installOnApplyLock(actuator, definition, false);
    }

    public boolean canPrepare(Item item) {
        return canPrepare(new ItemsImpl(item));
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
        Optional<Actuator> actuator = assigned(itemActuators, new ItemsImpl(item));
        return actuator.isPresent() && actuator.get().isRunning();
    }

    public boolean prepare(Actor actor, Item item) {
        return prepare(actor, new ItemsImpl(item), DefaultInstructions);
    }

    public boolean prepare(Actor actor, Item item, Consumer<Items> instructions) {
        return prepare(actor, new ItemsImpl(item), instructions, null);
    }

    public boolean prepare(Actor actor, Item item, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return prepare(actor, new ItemsImpl(item), duration, unit, instructions, null);
    }

    public boolean prepare(Actor actor, Item item, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        return prepare(actor, new ItemsImpl(item), duration, unit, instructions, instructionsAgain);
    }

    public boolean isPrepared(Items items) {
        return itemActuators.entrySet().stream().filter(element -> matchingItems.test(element.getKey(), items))
                .map(Entry::getValue).anyMatch(Actuator::isRunning);
    }

    public boolean prepare(Actor actor, Items items, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return prepare(actor, items, duration, unit, instructions, null);
    }

    public boolean prepare(Actor actor, Items items, Consumer<Items> instructions) {
        return prepare(actor, items, instructions, null);
    }

    public boolean prepare(Actor actor, Items items) {
        return prepare(actor, items, DefaultInstructions);
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
        if (isPrepared(items)) {
            Instructions definition = definitions(actor).get(items);
            // TODO implement partial item matching - just remove the items that aren't in use
            if (definition != null) {
                definitions(definition.actor).clear(definition.items);
            }
            definitions(actor).define(items,
                    new Instructions(actor, items, duration, unit, instructions, instructionsAgain));
            return false;
        } else if (canPrepare(items)) {
            Instructions definition = assign(actor, items, duration, unit, instructions, instructionsAgain);
            Optional<Actuator> actuator = chooseUnboundActuator(definition);
            if (actuator.isPresent()) {
                bind(actuator.get(), definition);
                if (!isDefault(definition)) {
                    acquireKeys(actuator.get(), definition);
                }
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

    public boolean clearAll(Actor actor) {
        DeviceInteractionDefinitions<Items, Instructions> definitions = definitions(actor);
        if (definitions.isEmpty()) {
            return false;
        } else {
            definitions.clear();
            return true;
        }
    }

    public boolean clear(Actor actor, Items items) {
        DeviceInteractionDefinitions<Items, Instructions> definitions = definitions(actor);
        if (definitions.isEmpty()) {
            return false;
        } else {
            return definitions.clear(items);
        }
    }

    private Optional<Actuator> chooseUnboundActuator(Instructions definition) {
        long duration = definition.durationSeconds;
        List<Actuator> matchingActuators = Actuators.matching(actuators, duration, TimeUnit.SECONDS);
        return matchingActuators.stream()
                .filter(Predicate.not(actuator -> actuatorItems.containsKey(actuatorKey(actuator)))).findFirst();
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

    private void unbind(Actuator actuator) {
        removeEvents(actuator);
        ItemEventAction applyEventAction = handledItems.remove(actuatorKey(actuator));
        if (applyEventAction != null) {
            events.itemApplied.remove(applyEventAction);
        }
        Items items = actuatorItems.remove(actuatorKey(actuator));
        itemActuators.remove(items);
    }

    public Optional<Actuator> getActuator(Item item) {
        return itemActuators.entrySet().stream().filter(entry -> entry.getKey().contains(item))
                .map(Entry<Items, Actuator>::getValue).findAny();
    }

    public Optional<Actuator> getActuator(Items items) {
        return itemActuators.entrySet().stream().filter(entry -> entry.getKey().containsAny(items))
                .map(Entry<Items, Actuator>::getValue).findAny();
    }

    private void installOnApplyLock(Actuator actuator, Instructions definition, boolean acquireAgain) {
        ItemEventAction action = events.when(definition.items).applied().thenOnce(() -> {
            handledItems.remove(actuatorKey(actuator));
            lockOnApply(actuator, definition, acquireAgain);
        });
        handledItems.put(actuatorKey(actuator), action);
    }

    private void lockOnApply(Actuator actuator, Instructions definition, boolean acquireAgain) {
        acquireKeysIfNecessary(actuator, definition, acquireAgain);
        Items handled = handledItems(actuator, definition);
        start(actuator, definition, handled);
    }

    private void installAfterApplyLock(Actuator actuator, Instructions definition) {
        events.when().beforeMessage().thenOnce(() -> {
            if (definition.items.anyApplied()) {
                lockAfterApply(actuator, definition);
            }
        });
    }

    private void lockAfterApply(Actuator actuator, Instructions definition) {
        Items handled = handledItems(actuator, definition, actuator.remaining(TimeUnit.SECONDS), TimeUnit.SECONDS);
        startAfterApply(actuator, definition, handled);
    }

    private void acquireKeysIfNecessary(Actuator actuator, Instructions definition, boolean acquireAgain) {
        if (!actuator.isRunning()) {
            if (acquireAgain) {
                acquireKeysAgain(actuator, definition);
            } else {
                acquireKeys(actuator, definition);
            }
        }
    }

    private Items handledItems(Actuator actuator, Instructions definition) {
        Items handled = teaseLib.relatedItems(Gadgets.Key_Release, definition.items);
        if (!handled.anyApplied()) {
            String actuatorName = actuatorName(actuator);
            handled.forEach(item -> item.applyTo(actuatorName)
                    .over(actuator.available(TimeUnit.SECONDS), TimeUnit.SECONDS).remember(Until.Removed));
        }
        return handled;
    }

    private Items handledItems(Actuator actuator, Instructions definition, long duration, TimeUnit unit) {
        Items handled = teaseLib.relatedItems(Gadgets.Key_Release, definition.items);
        if (!handled.anyApplied()) {
            String actuatorName = actuatorName(actuator);
            handled.forEach(item -> item.applyTo(actuatorName).over(duration, unit).remember(Until.Removed));
        }
        return handled;
    }

    private void start(Actuator actuator, Instructions definition, Items handled) {
        actuator.start();
        singleRenewHoldEvent(actuator);
        startCountdownAction(actuator, definition.items);
        installReleaseAction(actuator, definition.items, handled, definition);
    }

    private void startAfterApply(Actuator actuator, Instructions definition, Items handled) {
        actuator.start();
        startCountdownAction(actuator, definition.items);
        Optional<Long> durationSeconds = durationSeconds(definition.items);
        if (durationSeconds.isPresent()) {
            actuator.start(durationSeconds.get(), TimeUnit.SECONDS);
            removeEvents(actuator);
        } else {
            singleRenewHoldEvent(actuator);
        }

        installReleaseAction(actuator, definition.items, handled, definition);
    }

    private ItemEventAction startCountdownAction(Actuator actuator, Items items) {
        return installedCountDownActions.computeIfAbsent(actuatorKey(actuator), key //
        -> events.when(items).remember().thenOnce(() -> {
            if (items.anyAre(Until.Expired) && !duration(items).expired()) {
                removeEvent(actuatorKey(actuator), events.afterPrompt, installedRenewHoldEvents);
                startCountDown(actuator, items);
            }
        }));
    }

    private static Duration duration(Items items) {
        return items.stream().map(Item::duration).max(Duration::compare).orElse(Duration.Expired);
    }

    private Event<ScriptEventArgs> singleRenewHoldEvent(Actuator actuator) {
        return installedRenewHoldEvents.computeIfAbsent(actuatorKey(actuator), key //
        -> events.afterPrompt.add(eventArgs -> renewHold(actuator)));
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
            String actuatorName = actuatorName(actuator);
            handled.stream().forEach(item -> item.removeFrom(actuatorName));
            removeEvents(actuator);

            Actor currentActor = scriptRenderer.currentActor();
            Optional<Instructions> updated = definitions(currentActor).findMatching(items);
            if (updated.isPresent() && definition.isPresent()) {
                // doesn't work on restore() but then there's no previous actor anyway
                if (definition.get().actor != currentActor) {
                    passKeys(currentActor, actuator, items);
                }
            }

            if (updated.isPresent()) {
                Optional<Consumer<Items>> acquireKeysAgain = updated.get().acquireKeysAgain;
                if (acquireKeysAgain.isPresent()) {
                    acquireKeysAgain(actuator, updated.get());
                    installOnApplyLock(actuator, updated.get(), true);
                } else {
                    unbind(actuator);
                    // doesn't work on restore() but this is code path is not called at startup
                    prepare(definition.get().actor, items, DefaultInstructions);
                }
            }
        });
    }

    private void removeEvents(Actuator actuator) {
        removeEvent(actuatorKey(actuator), events.itemRemember, installedCountDownActions);
        removeEvent(actuatorKey(actuator), events.afterPrompt, installedRenewHoldEvents);
    }

    private static <K, S extends EventArgs, T extends Event<S>, E extends EventSource<S>> void removeEvent( //
            K key, E eventSource, Map<K, T> map) {
        T event = map.get(key);
        if (eventSource.contains(event)) {
            eventSource.remove(event);
            map.remove(key);
        }
    }

    private static void startCountDown(Actuator actuator, Items items) {
        long seconds = durationSeconds(items).orElseThrow();
        actuator.start(seconds, TimeUnit.SECONDS);
    }

    private static Optional<Long> durationSeconds(Items items) {
        return items.stream().filter(item -> !item.expired()).map(Item::duration)
                .map(duration -> duration.remaining(TimeUnit.SECONDS)).reduce(Math::min);
    }

    private static void renewHold(Actuator actuator) {
        if (actuator.isRunning()) {
            actuator.hold();
        }
    }

}
