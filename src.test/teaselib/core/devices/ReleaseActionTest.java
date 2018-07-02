package teaselib.core.devices;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import teaselib.Household;
import teaselib.Material;
import teaselib.State;
import teaselib.Toys;
import teaselib.core.TeaseLib;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.state.StateProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Storage;
import teaselib.core.util.QualifiedItem;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class ReleaseActionTest {
    public static final class ReleaseActionState extends ReleaseAction {
        static final AtomicBoolean Success = new AtomicBoolean(false);
        boolean removed = false;

        public ReleaseActionState(Storage storage) {
            super(storage);
        }

        public ReleaseActionState(TeaseLib teaseLib, String item) {
            super(teaseLib, TeaseLib.DefaultDomain, item);
        }

        @Override
        public void performAction() {
            Success.set(true);
            removed = true;
        }
    }

    @Test
    public void testReleaseActionStateQualified() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(actionItem).toString());
        assertEquals(actionItem, sameInstance);

        String action = Persist.persist(actionItem);
        ReleaseActionState restored = (ReleaseActionState) Persist.from(action, clazz -> script.teaseLib);
        assertEquals(actionItem, restored);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);

        // start(action);

        restraints.remove();

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));
        assertEquals(false, restored.removed);
    }

    @Test
    public void testReleaseActionInstance() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(actionItem).toString());
        assertEquals(actionItem, sameInstance);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(actionItem);
        assertTrue(restraints.is(script.namespace));
        assertTrue(actionItem.is(script.namespace));

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
    public void testReleaseActionInstanceRemoveOtherToyOfSameType() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(actionItem).toString());
        assertEquals(actionItem, sameInstance);

        Item handCuffs = script.items(Toys.Wrist_Restraints).query(Material.Metal).get();
        handCuffs.apply();
        handCuffs.applyTo(actionItem);
        assertTrue(handCuffs.is(script.namespace));
        assertTrue(handCuffs.is(script.namespace));

        // start(action);

        Item otherCuffs = script.items(Toys.Wrist_Restraints).query(Material.Leather).get();
        assertNotEquals(handCuffs, otherCuffs);
        otherCuffs.remove();

        assertFalse(otherCuffs.applied());
        assertFalse(otherCuffs.is(script.namespace));
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
        State sameInstance1 = script.state(QualifiedItem.of(removeRestraintsAction).toString());
        assertEquals(removeRestraintsAction, sameInstance1);

        ReleaseActionState removeChainsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath2));
        State sameInstance2 = script.state(QualifiedItem.of(removeChainsAction).toString());
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
    public void testReleaseActionMultiLevelReleaseWhenOnlySingleItemIsRemoved() {
        TestScript script = TestScript.getOne();

        String devicePath1 = "KeyRelease/MyPhoton/1";
        String devicePath2 = "KeyRelease/MyPhoton/2";

        ReleaseActionState removeRestraintsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath1));
        State sameInstance1 = script.state(QualifiedItem.of(removeRestraintsAction).toString());
        assertEquals(removeRestraintsAction, sameInstance1);

        ReleaseActionState removeChainsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath2));
        State sameInstance2 = script.state(QualifiedItem.of(removeChainsAction).toString());
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

        Item someChains = chains.item(Toys.Chains);
        someChains.remove();
        assertTrue(removeChainsAction.removed);
        assertFalse(removeRestraintsAction.removed);
        assertTrue(chains.anyApplied());
        assertFalse(chains.allApplied());
        assertTrue(restraints.allApplied());

        Item wristRestraints = restraints.item(Toys.Wrist_Restraints);
        wristRestraints.remove();
        assertTrue(removeChainsAction.removed);
        assertTrue(removeRestraintsAction.removed);
        assertTrue(chains.anyApplied());
        assertTrue(restraints.anyApplied());
        assertFalse(restraints.allApplied());
    }

    @Test
    public void testThatReleaseActionCanBeAttachedBeforehandWithoutApplyingToPeer() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = script.teaseLib.state(TeaseLib.DefaultDomain,
                new ReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(actionItem).toString());
        assertEquals(actionItem, sameInstance);

        Item restraints = script.item(Toys.Wrist_Restraints);
        assertFalse(actionItem.applied());
        new StateProxy(script.namespace, actionItem).applyTo(restraints);
        assertTrue(actionItem.is(script.namespace));

        assertFalse(restraints.applied());
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
