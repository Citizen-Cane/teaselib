package teaselib.core.devices.release;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import teaselib.core.DeviceInteractionImplementations;
import teaselib.core.ItemsImpl;
import teaselib.core.Script;
import teaselib.core.ScriptInteraction;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author Citizen-Cane
 *
 */
public class KeyReleaseSetup implements ScriptInteraction {

    /**
     * The default instruction actually don't instruct anything.
     * <p>
     * Instead the release actuator is activated when applying items.
     */
    public static final Consumer<Items> DefaultInstructions = KeyReleaseDeviceInteraction.DefaultInstructions;

    private final Script script;
    public final KeyReleaseDeviceInteraction deviceInteraction;

    public KeyReleaseSetup(Script script) {
        this.script = script;
        this.deviceInteraction = script.teaseLib.globals.get(DeviceInteractionImplementations.class)
                .get(KeyReleaseDeviceInteraction.class);
    }

    public boolean deviceAvailable() {
        return deviceInteraction.deviceAvailable();
    }

    public boolean devicesAvailable(Items... items) {
        return deviceInteraction.devicesAvailable(items);
    }

    public boolean canPrepare(Item item) {
        return deviceInteraction.canPrepare(new ItemsImpl(item));
    }

    public boolean canPrepare(Items items) {
        return deviceInteraction.canPrepare(items);
    }

    public boolean isPrepared(Item item) {
        return deviceInteraction.isPrepared(item);
    }

    public boolean prepare(Item item) {
        return deviceInteraction.prepare(script.actor, item);
    }

    public boolean prepare(Item item, Consumer<Items> instructions) {
        return deviceInteraction.prepare(script.actor, item, instructions);
    }

    public boolean prepare(Item item, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return deviceInteraction.prepare(script.actor, item, duration, unit, instructions);
    }

    public boolean prepare(Item item, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        return deviceInteraction.prepare(script.actor, item, duration, unit, instructions, instructionsAgain);
    }

    public boolean isPrepared(Items items) {
        return deviceInteraction.isPrepared(items);
    }

    public boolean prepare(Items items, long duration, TimeUnit unit, Consumer<Items> instructions) {
        return deviceInteraction.prepare(script.actor, items, duration, unit, instructions);
    }

    public boolean prepare(Items items) {
        return deviceInteraction.prepare(script.actor, items);
    }

    public boolean prepare(Items items, Consumer<Items> instructions) {
        return deviceInteraction.prepare(script.actor, items, instructions);
    }

    public boolean prepare(Items items, long duration, TimeUnit unit, Consumer<Items> instructions,
            Consumer<Items> instructionsAgain) {
        return deviceInteraction.prepare(script.actor, items, duration, unit, instructions, instructionsAgain);
    }

    public boolean prepare(Items items, Consumer<Items> instructions, Consumer<Items> instructionsAgain) {
        return deviceInteraction.prepare(script.actor, items, instructions, instructionsAgain);
    }

    public boolean clearAll() {
        return deviceInteraction.clearAll(script.actor);
    }

    public boolean clear(Items items) {
        return deviceInteraction.clear(script.actor, items);
    }

}
