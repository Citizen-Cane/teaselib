package teaselib.core.devices;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import teaselib.Household;
import teaselib.State;
import teaselib.Toys;
import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.util.Persist;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.ReflectionUtils;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class ReleaseActionTest {
    public static final class ReleaseActionState extends StateImpl implements Persist.Persistable {
        static final AtomicBoolean Success = new AtomicBoolean(false);

        boolean removed = false;

        public ReleaseActionState(TeaseLib teaseLib, String devicePath) {
            super(teaseLib, TeaseLib.DefaultDomain,
                    ReflectionUtils.normalizeClassName(ReleaseActionState.class) + "." + devicePath);
        }

        @Override
        public List<String> persisted() {
            return Arrays.asList(Persist.persist(domain), Persist.persist(item.toString()));
        }

        public ReleaseActionState(Persist.Storage storage) {
            super(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
        }

        public String getStateName() {
            return QualifiedItem.of(item).toString();
        }

        public String devicePath() {
            return QualifiedItem.nameOf(item);
        }

        @Override
        public Persistence remove() {
            Success.set(true);
            removed = true;
            return super.remove();
        }
    }

    @Test
    public void testReleaseActionStateQualified() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(actionItem.getStateName());
        assertEquals(actionItem, sameInstance);

        String action = Persist.persist(actionItem);
        ReleaseActionState restored = (ReleaseActionState) Persist.from(action, clazz -> script.teaseLib);
        assertEquals(devicePath, restored.devicePath());

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);

        // start(action);

        restraints.remove();

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));

        // TODO persisted instance is restored multiple times
        // -> doesn't matter for physical device but local state won't be restored
        assertEquals(false, restored.removed);
    }

    @Test
    public void testReleaseActionInstance() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(actionItem.getStateName());
        assertEquals(actionItem, sameInstance);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(actionItem);

        // start(action);

        restraints.remove();

        assertFalse(restraints.applied());
        assertFalse(restraints.is(script.namespace));
        assertFalse(actionItem.is(script.namespace));
        assertFalse(actionItem.applied());

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));
        assertEquals(true, actionItem.removed);
    }

    @Test
    public void testReleaseActionMultiLevel() {
        TestScript script = TestScript.getOne();

        String devicePath1 = "KeyRelease/MyPhoton/1";
        String devicePath2 = "KeyRelease/MyPhoton/2";

        ReleaseActionState removeRestraintsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath1));
        State sameInstance1 = script.state(removeRestraintsAction.getStateName());
        assertEquals(removeRestraintsAction, sameInstance1);

        ReleaseActionState removeChainsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath2));
        State sameInstance2 = script.state(removeChainsAction.getStateName());
        assertEquals(removeChainsAction, sameInstance2);

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        restraints.apply();
        restraints.applyTo(removeRestraintsAction);

        Items chains = script.items(Toys.Chains, Household.Bell);
        chains.apply();
        chains.applyTo(removeChainsAction);
        for (Item restraint : restraints) {
            chains.applyTo(restraint);
        }

        // start(action);

        assertTrue(chains.allApplied());
        assertTrue(restraints.allApplied());
        assertEquals(false, removeChainsAction.removed);
        assertEquals(false, removeRestraintsAction.removed);

        chains.remove();
        assertEquals(true, removeChainsAction.removed);
        assertEquals(false, removeRestraintsAction.removed);
        assertFalse(chains.anyApplied());
        assertTrue(restraints.allApplied());

        restraints.remove();
        assertEquals(true, removeChainsAction.removed);
        assertEquals(true, removeRestraintsAction.removed);
        assertFalse(chains.anyApplied());
        assertFalse(restraints.anyApplied());
    }

    @Test
    public void testThatReleaseActionCanBeAppliedBeforehand() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(actionItem.getStateName());
        assertEquals(actionItem, sameInstance);

        Item restraints = script.item(Toys.Wrist_Restraints);
        assertFalse(actionItem.applied());
        actionItem.applyTo(restraints);

        // TODO namespace not assigned because we don't have a proxy
        assertFalse(actionItem.is(script.namespace));

        // TODO Should be false since we've applied the action item only - ignore action items when testing applied()
        // state
        // assertFalse(restraints.applied());
        assertTrue(restraints.applied());
        assertTrue(actionItem.applied());

        restraints.apply();
        assertTrue(restraints.applied());
        assertTrue(restraints.is(script.namespace));
        assertTrue(actionItem.is(script.namespace));
        assertTrue(actionItem.applied());
        assertTrue(actionItem.is(Toys.Wrist_Restraints));

        // start(action);

        restraints.remove();
        assertFalse(restraints.applied());
        assertFalse(restraints.is(script.namespace));
        // TODO currently this works as intended because we don't use a proxy for setting the namespace
        assertFalse(actionItem.is(script.namespace));
        assertFalse(actionItem.is(Toys.Wrist_Restraints));
        assertFalse(actionItem.applied());

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));
        assertEquals(true, actionItem.removed);
    }

    @Test
    @Ignore
    // TODO simulate device
    public void testActuatorReleaseAction() {
        TestScript script = TestScript.getOne();

        KeyRelease device = script.teaseLib.devices.get(KeyRelease.class).getDefaultDevice();

        assertTrue(device.active());
        assertTrue(device.actuators().size() > 0);

        Actuator actuator = device.actuators().get(0);
        State release = actuator.releaseAction(script.teaseLib);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(release);

        // start(actuator);

        assertTrue(device.active());
        assertTrue(actuator.isRunning());
        restraints.remove();
        assertFalse(actuator.isRunning());

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));
    }
}
