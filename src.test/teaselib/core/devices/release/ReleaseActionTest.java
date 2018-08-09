package teaselib.core.devices.release;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.Household;
import teaselib.MainScript;
import teaselib.Material;
import teaselib.Sexuality.Gender;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.devices.ReleaseAction;
import teaselib.core.state.StateProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.Persist.Storage;
import teaselib.core.util.QualifiedItem;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class ReleaseActionTest {
    public static final class TestReleaseActionState extends ReleaseAction {
        static final AtomicBoolean Success = new AtomicBoolean(false);
        boolean removed = false;

        public TestReleaseActionState(Storage storage) {
            super(storage, TestReleaseActionState.class);
        }

        public TestReleaseActionState(TeaseLib teaseLib, String item) {
            super(teaseLib, TeaseLib.DefaultDomain, item, TestReleaseActionState.class);
        }

        @Override
        public boolean performAction() {
            removed = true;
            Success.set(removed);
            return removed;
        }
    }

    @Before
    public void resetGlobalFlag() {
        TestReleaseActionState.Success.set(false);
    }

    @Test
    public void testReleaseActionStateQualifiedPersisted() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        TestReleaseActionState actionState = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(actionState).toString());
        assertEquals(actionState, sameInstance);

        String action = Persist.persist(actionState);
        TestReleaseActionState restored = (TestReleaseActionState) Persist.from(action, clazz -> script.teaseLib);
        assertEquals(actionState, restored);
        assertNotSame(actionState, restored);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);

        restraints.remove();

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
        assertEquals(true, actionState.removed);
        // different instance
        assertEquals(false, restored.removed);
    }

    @Test
    public void testReleaseActionStatesAreSingletons() {
        TestScript script = TestScript.getOne();
        String domain = TeaseLib.DefaultDomain;
        String devicePath = "KeyRelease/MyPhoton/1";

        String action = Persist.persistedInstance(TestReleaseActionState.class, Arrays.asList(domain, devicePath));
        State actionState1 = script.state(action);
        State actionState2 = script.state(action);

        assertEquals(actionState1, actionState2);
        assertSame(((StateProxy) actionState1).state, ((StateProxy) actionState2).state);
    }

    @Test
    public void testReleaseActionStateQualifiedPersistedDirectly() {
        TestScript script = TestScript.getOne();
        String domain = TeaseLib.DefaultDomain;
        String devicePath = "KeyRelease/MyPhoton/1";

        String action = Persist.persistedInstance(TestReleaseActionState.class, Arrays.asList(domain, devicePath));
        State actionState = script.state(action);
        assertFalse(actionState.applied());

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);
        assertTrue(actionState.applied());

        restraints.remove();
        assertFalse(actionState.applied());
    }

    @Test
    public void testReleaseActionInstance() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        TestReleaseActionState releaseAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(releaseAction).toString());
        assertEquals(releaseAction, sameInstance);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(releaseAction);
        assertTrue(restraints.is(script.namespace));
        assertTrue(releaseAction.is(script.namespace));

        // start(action);

        restraints.remove();

        assertFalse(restraints.applied());
        assertFalse(restraints.is(script.namespace));
        assertFalse(releaseAction.is(script.namespace));
        assertFalse(releaseAction.applied());

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
        assertEquals(true, releaseAction.removed);
    }

    public static class BuggyScript extends TeaseScript implements MainScript {
        public BuggyScript(TeaseLib teaseLib) {
            super(teaseLib, new ResourceLoader(BuggyScript.class), teaseLib.getDominant(Gender.Feminine, Locale.UK),
                    "test");
        }

        @Override
        public void run() {
            String devicePath = "KeyRelease/MyPhoton/1";
            TestReleaseActionState releaseAction = teaseLib.state(TeaseLib.DefaultDomain,
                    new TestReleaseActionState(teaseLib, devicePath));

            Item restraints = item(Toys.Wrist_Restraints);
            restraints.apply();
            restraints.applyTo(releaseAction);

            assertEquals(false, TestReleaseActionState.Success.getAndSet(false));

            throw new IllegalStateException();
        }
    }

    @Test
    public void testReleaseActionInstanceErrorHandling() throws Exception {
        TestScript script = TestScript.getOne();
        try {
            script.teaseLib.run(BuggyScript.class.getName());
        } catch (Exception e) {
            if (!(e instanceof IllegalStateException)) {
                throw e;
            }
        }

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
    }

    public static class BugFreeScript extends TeaseScript implements MainScript {
        public BugFreeScript(TeaseLib teaseLib) {
            super(teaseLib, new ResourceLoader(BuggyScript.class), teaseLib.getDominant(Gender.Feminine, Locale.UK),
                    "test");
        }

        @Override
        public void run() {
            String devicePath = "KeyRelease/MyPhoton/1";
            TestReleaseActionState releaseAction = teaseLib.state(TeaseLib.DefaultDomain,
                    new TestReleaseActionState(teaseLib, devicePath));

            Item restraints = item(Toys.Wrist_Restraints);
            restraints.apply();
            restraints.applyTo(releaseAction);
        }
    }

    @Test
    public void testReleaseActionInstanceScriptSuccessfulFinish() throws ReflectiveOperationException {
        TestScript script = TestScript.getOne();
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));

        script.teaseLib.run(BugFreeScript.class.getName());
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));
    }

    @Test
    public void testReleaseActionStatesWorkAsSingletons() {
        TestScript script = TestScript.getOne();
        String devicePath = "KeyRelease/MyPhoton/1";

        TestReleaseActionState actionState1 = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath));
        TestReleaseActionState actionState2 = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath));
        assertSame(actionState1, actionState2);

        TestReleaseActionState actionState3 = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath));
        assertSame(actionState3, actionState1);
        TestReleaseActionState actionState4a = new TestReleaseActionState(script.teaseLib, devicePath);
        assertSame(actionState3, script.teaseLib.state(TeaseLib.DefaultDomain, actionState4a));
        actionState3.apply();
        assertSame(actionState3, script.teaseLib.state(TeaseLib.DefaultDomain, actionState4a));
    }

    @Test
    public void testReleaseActionInstanceCanBeUsedMultuipleTimes() throws ReflectiveOperationException {
        TestScript script = TestScript.getOne();

        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));
        script.teaseLib.run(BugFreeScript.class.getName());
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));

        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));
        // TODO must be able to apply release actions multiple times
        script.teaseLib.run(BugFreeScript.class.getName());
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));
    }

    @Test
    public void testReleaseActionInstanceRemoveOtherToyOfSameType() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();

        String devicePath = "KeyRelease/MyPhoton/1";
        TestReleaseActionState releaseAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(releaseAction).toString());
        assertEquals(releaseAction, sameInstance);

        Item handCuffs = script.items(Toys.Wrist_Restraints).query(Material.Metal).get();
        handCuffs.apply();
        handCuffs.applyTo(releaseAction);
        assertTrue(handCuffs.is(script.namespace));
        assertTrue(handCuffs.is(script.namespace));

        // start(action);

        Item otherCuffs = script.items(Toys.Wrist_Restraints).query(Material.Leather).get();
        assertNotEquals(handCuffs, otherCuffs);
        otherCuffs.remove();

        assertFalse(otherCuffs.applied());
        assertFalse(otherCuffs.is(script.namespace));
        assertFalse(releaseAction.is(script.namespace));
        assertFalse(releaseAction.applied());

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
        assertEquals(true, releaseAction.removed);
    }

    @Test
    public void testReleaseActionMultiLevel() {
        TestScript script = TestScript.getOne();

        String devicePath1 = "KeyRelease/MyPhoton/1";
        String devicePath2 = "KeyRelease/MyPhoton/2";

        TestReleaseActionState removeRestraintsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath1));
        State sameInstance1 = script.state(QualifiedItem.of(removeRestraintsAction).toString());
        assertEquals(removeRestraintsAction, sameInstance1);

        TestReleaseActionState removeChainsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath2));
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

        TestReleaseActionState removeRestraintsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath1));
        State sameInstance1 = script.state(QualifiedItem.of(removeRestraintsAction).toString());
        assertEquals(removeRestraintsAction, sameInstance1);

        TestReleaseActionState removeChainsAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath2));
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

        Item someChains = chains.get(Toys.Chains);
        someChains.remove();
        assertTrue(removeChainsAction.removed);
        assertFalse(removeRestraintsAction.removed);
        assertTrue(chains.anyApplied());
        assertFalse(chains.allApplied());
        assertTrue(restraints.allApplied());

        Item wristRestraints = restraints.get(Toys.Wrist_Restraints);
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
        State releaseAction = script.teaseLib.state(TeaseLib.DefaultDomain,
                new TestReleaseActionState(script.teaseLib, devicePath));
        State sameInstance = script.state(QualifiedItem.of(releaseAction).toString());
        assertEquals(releaseAction, sameInstance);

        Item restraints = script.item(Toys.Wrist_Restraints);
        assertFalse(releaseAction.applied());

        StateProxy productionState = new StateProxy(script.namespace, releaseAction);
        productionState.applyTo(restraints);
        assertTrue(releaseAction.is(script.namespace));

        assertFalse(restraints.applied());
        assertTrue(releaseAction.applied());

        restraints.apply();
        assertTrue(restraints.applied());
        assertTrue(restraints.is(script.namespace));
        assertTrue(releaseAction.is(script.namespace));
        assertTrue(releaseAction.applied());
        assertTrue(releaseAction.is(Toys.Wrist_Restraints));

        // start(action);

        restraints.remove();
        assertFalse(restraints.applied());
        assertFalse(restraints.is(script.namespace));
        assertFalse(releaseAction.is(script.namespace));
        assertFalse(releaseAction.is(Toys.Wrist_Restraints));
        assertFalse(releaseAction.applied());

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
        assertEquals(true, ((TestReleaseActionState) releaseAction).removed);
    }

    @Test
    public void ensureThatActuatorAlwaysReturnsTheSameReleaseActionInstance() {
        TestScript script = TestScript.getOne();

        Actuator a = new Actuator(new KeyRelease(null, null, null) {
            @Override
            public String getDevicePath() {
                return "KeyRelease/10.1.1.11/Test";
            }
        }, 0);

        State releaseAction1 = a.releaseAction(script);
        State releaseAction2 = a.releaseAction(script);
        assertEquals(releaseAction1, releaseAction2);
        assertNotSame(releaseAction1, releaseAction2);

        State state1 = ((StateProxy) releaseAction1).state;
        State state2 = ((StateProxy) releaseAction2).state;
        assertEquals(state1, state2);
        assertSame(state1, state2);
        // Instances are indeed identical:
        // - firstly they're proxies in order to tag items with the name space
        // - secondly, they're constructed anew each time since Actuator can't cache them (TeaseLib does)
        // - but thirdly, teaselib caches them and returns the existing instance
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
        State release = actuator.releaseAction(script);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(release);

        // start(actuator);

        assertTrue(device.active());
        assertTrue(actuator.isRunning());
        restraints.remove();
        assertFalse(actuator.isRunning());

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
    }
}
