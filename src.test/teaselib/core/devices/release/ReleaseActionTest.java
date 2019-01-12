package teaselib.core.devices.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.Accessoires;
import teaselib.MainScript;
import teaselib.Material;
import teaselib.Sexuality.Gender;
import teaselib.State;
import teaselib.TeaseScript;
import teaselib.Toys;
import teaselib.core.ResourceLoader;
import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.devices.ReleaseAction;
import teaselib.core.devices.remote.RemoteDevices;
import teaselib.core.state.StateProxy;
import teaselib.core.util.Persist;
import teaselib.core.util.ReflectionUtils;
import teaselib.core.util.Storage;
import teaselib.test.TestDeviceFactory;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author Citizen-Cane
 *
 */
public class ReleaseActionTest {
    public static final class TestReleaseActionState extends ReleaseAction {
        static final AtomicBoolean Success = new AtomicBoolean(false);
        boolean removed = false;

        public TestReleaseActionState(Storage storage) {
            super(storage, TestReleaseActionState.class);
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

    private static String getTestReleaseAction(String domain, String devicePath) {
        return Persist.persistedInstance(TestReleaseActionState.class, Arrays.asList(domain, devicePath));
    }

    @Test
    public void testReleaseActionStatesAreSingletons() {
        TestScript script = TestScript.getOne();
        String domain = TeaseLib.DefaultDomain;
        String devicePath = "KeyRelease/MyPhoton/1";

        String action = getTestReleaseAction(domain, devicePath);
        State actionState1 = script.state(action);
        State actionState2 = script.state(action);

        assertEquals(actionState1, actionState2);
        assertSame(((StateProxy) actionState1).state, ((StateProxy) actionState2).state);
    }

    @Test
    public void testThatActionStatesCannotBeInstanciatedFromQualifiedString() {
        TestScript script = TestScript.getOne();
        String domain = TeaseLib.DefaultDomain;
        String devicePath1 = "KeyRelease/MyPhoton/1";
        String devicePath2 = "KeyRelease/MyPhoton/2";

        String action = getTestReleaseAction(domain, devicePath1);
        State actionState1 = script.state(action);
        State actionState2 = script.state(ReflectionUtils.qualified(TestReleaseActionState.class, devicePath2));

        assertEquals(TestReleaseActionState.class, ((StateProxy) actionState1).state.getClass());
        assertEquals(StateImpl.class, ((StateProxy) actionState2).state.getClass());
    }

    @Test
    public void testReleaseActionStateQualifiedPersistedDirectly() {
        TestScript script = TestScript.getOne();
        String domain = TeaseLib.DefaultDomain;
        String devicePath = "KeyRelease/MyPhoton/1";

        String action = getTestReleaseAction(domain, devicePath);
        State releaseAction = script.state(action);
        assertFalse(releaseAction.applied());

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);
        assertTrue(releaseAction.applied());
        assertTrue(restraints.is(script.namespace));
        assertTrue(releaseAction.is(script.namespace));

        restraints.remove();
        assertFalse(releaseAction.applied());
        assertFalse(restraints.is(script.namespace));
        assertFalse(releaseAction.is(script.namespace));
        assertFalse(releaseAction.applied());
    }

    public static abstract class BuggyScript extends TeaseScript implements MainScript {
        public BuggyScript(TeaseLib teaseLib) {
            super(teaseLib, new ResourceLoader(BuggyScript.class), TestScript.newActor(Gender.Feminine), "test");
        }

        @Override
        public void run() {
            String domain = TeaseLib.DefaultDomain;
            String devicePath = "KeyRelease/MyPhoton/1";
            State releaseAction = teaseLib.state(domain, getTestReleaseAction(domain, devicePath));

            Item restraints = item(Toys.Wrist_Restraints);
            restraints.apply();
            state(Toys.Wrist_Restraints).applyTo(releaseAction);

            throwSomething();
        }

        public abstract void throwSomething();
    }

    public static class BuggyScript1 extends BuggyScript {
        public BuggyScript1(TeaseLib teaseLib) {
            super(teaseLib);
        }

        public class TestException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

        @Override
        public void throwSomething() {
            throw new TestException();
        }
    }

    @Test
    public void testReleaseActionInstanceErrorHandlingException() throws Exception {
        TestScript script = TestScript.getOne();
        try {
            script.teaseLib.run(BuggyScript1.class.getName());
        } catch (BuggyScript1.TestException e) {
            assertEquals("Release actions not triggered on unhandled script exception", true,
                    TestReleaseActionState.Success.getAndSet(false));
        }
    }

    public static class BuggyScript2 extends BuggyScript {
        public BuggyScript2(TeaseLib teaseLib) {
            super(teaseLib);
        }

        public class TestError extends Error {
            private static final long serialVersionUID = 1L;
        }

        @Override
        public void throwSomething() {
            throw new TestError();
        }
    }

    @Test
    public void testReleaseActionInstanceErrorHandlingError() throws Exception {
        TestScript script = TestScript.getOne();
        try {
            script.teaseLib.run(BuggyScript2.class.getName());
        } catch (BuggyScript2.TestError e) {
            assertEquals("Release actions not triggered on unhandled script error", true,
                    TestReleaseActionState.Success.getAndSet(false));
        }
    }

    public static class BugFreeScript extends TeaseScript implements MainScript {
        public BugFreeScript(TestScript script) {
            super(script);
        }

        public BugFreeScript(TeaseLib teaseLib) {
            super(teaseLib, new ResourceLoader(BuggyScript.class), TestScript.newActor(Gender.Feminine), "test");
        }

        @Override
        public void run() {
            String domain = TeaseLib.DefaultDomain;
            String devicePath = "KeyRelease/MyPhoton/1";
            State releaseAction = teaseLib.state(domain, getTestReleaseAction(domain, devicePath));

            Item restraints = item(Toys.Wrist_Restraints);
            restraints.apply();
            restraints.applyTo(releaseAction);
        }
    }

    @Test
    public void testReleaseActionInstanceScriptSuccessfulFinish() throws ReflectiveOperationException {
        TestScript script = TestScript.getOne();
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));

        script.script(BugFreeScript.class).run();
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));
    }

    @Test
    public void testReleaseActionInstanceCanBeUsedMultuipleTimes() throws ReflectiveOperationException {
        TestScript script = TestScript.getOne();

        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));
        script.script(BugFreeScript.class).run();
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));

        script.script(BugFreeScript.class).run();
        assertEquals(false, TestReleaseActionState.Success.getAndSet(false));
    }

    @Test
    public void testReleaseActionInstanceRemoveDefaultToy() {
        TestScript script = TestScript.getOne();
        script.addTestUserItems();
        script.setAvailable(Toys.values());

        String domain = TeaseLib.DefaultDomain;
        String devicePath = "KeyRelease/MyPhoton/1";
        State releaseAction = script.teaseLib.state(domain, getTestReleaseAction(domain, devicePath));

        Item handCuffs = script.items(Toys.Wrist_Restraints).matching(Material.Metal).get();
        handCuffs.apply();
        handCuffs.applyTo(releaseAction);
        assertTrue(handCuffs.applied());
        assertTrue(handCuffs.is(script.namespace));

        Item defaultCuffs = script.item(Toys.Wrist_Restraints);
        assertTrue(defaultCuffs.applied());
        defaultCuffs.remove();
        assertEquals(handCuffs, defaultCuffs);

        assertFalse(defaultCuffs.applied());
        assertFalse(defaultCuffs.is(script.namespace));
        assertFalse(releaseAction.is(script.namespace));
        assertFalse(releaseAction.applied());

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
    }

    @Test
    public void testReleaseActionMultiLevel() {
        TestScript script = TestScript.getOne();

        String domain = TeaseLib.DefaultDomain;
        String devicePath1 = "KeyRelease/MyPhoton/1";
        String devicePath2 = "KeyRelease/MyPhoton/2";

        State removeRestraintsAction = script.teaseLib.state(domain, getTestReleaseAction(domain, devicePath1));
        State removeChainsAction = script.teaseLib.state(domain, getTestReleaseAction(domain, devicePath2));

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        restraints.apply();
        restraints.applyTo(removeRestraintsAction);

        Items chains = script.items(Toys.Chains, Accessoires.Bells);
        chains.apply();
        chains.applyTo(removeChainsAction);
        for (Item restraint : restraints) {
            chains.applyTo(restraint);
        }

        assertTrue(chains.allApplied());
        assertTrue(restraints.allApplied());
        assertTrue(removeChainsAction.applied());
        assertTrue(removeRestraintsAction.applied());

        chains.remove();
        // TODO There are still restraints peers attached to the chains and bell
        // - all peers but the bell' action state are removed on removing the restraints
        assertFalse(removeChainsAction.applied());
        assertTrue(removeRestraintsAction.applied());
        assertFalse(chains.anyApplied());
        assertTrue(restraints.allApplied());

        restraints.remove();
        assertFalse(removeChainsAction.applied());
        assertFalse(removeRestraintsAction.applied());
        assertFalse(chains.anyApplied());
        assertFalse(restraints.anyApplied());
    }

    @Test
    public void testReleaseActionMultiLevelReleaseWhenOnlySingleItemIsRemoved() {
        TestScript script = TestScript.getOne();

        String domain = TeaseLib.DefaultDomain;
        String devicePath1 = "KeyRelease/MyPhoton/1";
        String devicePath2 = "KeyRelease/MyPhoton/2";

        State removeRestraintsAction = script.teaseLib.state(domain, getTestReleaseAction(domain, devicePath1));
        State removeChainsAction = script.teaseLib.state(domain, getTestReleaseAction(domain, devicePath2));

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        restraints.apply();
        restraints.applyTo(removeRestraintsAction);

        Items chains = script.items(Toys.Chains, Accessoires.Bells);
        chains.applyTo(removeChainsAction);
        chains.applyTo(restraints);

        assertTrue(chains.allApplied());
        assertTrue(restraints.allApplied());
        assertTrue(removeChainsAction.applied());
        assertTrue(removeRestraintsAction.applied());

        Item singleChainItem = chains.get(Toys.Chains);
        // TODO remove also removes the bell guid,
        // but the bell is still attached to restraints
        assertTrue(chains.get(Accessoires.Bells).applied());
        assertTrue(chains.get(Accessoires.Bells).is(Toys.Wrist_Restraints));
        // assertTrue(chains.get(Household.Bell).is(restraints.get(Toys.Wrist_Restraints)));
        // assertTrue(chains.get(Household.Bell).is(restraints));
        singleChainItem.remove();
        assertTrue(chains.get(Accessoires.Bells).applied());
        assertTrue(chains.get(Accessoires.Bells).is(Toys.Wrist_Restraints));
        // assertTrue(chains.get(Household.Bell).is(restraints.get(Toys.Wrist_Restraints)));
        // assertTrue(chains.get(Household.Bell).is(restraints));

        assertFalse(removeChainsAction.applied());
        assertTrue(removeRestraintsAction.applied());

        assertTrue(chains.anyApplied());
        assertFalse(chains.allApplied());
        assertTrue(restraints.allApplied());

        Item wristRestraints = restraints.get(Toys.Wrist_Restraints);
        wristRestraints.remove();
        assertFalse(removeChainsAction.applied());
        assertFalse(removeRestraintsAction.applied());
        assertTrue(chains.anyApplied());
        assertTrue(restraints.anyApplied());
        assertFalse(restraints.allApplied());
    }

    @Test
    public void testThatReleaseActionCanBeAttachedBeforehandWithoutApplyingToPeer() {
        TestScript script = TestScript.getOne();

        String domain = TeaseLib.DefaultDomain;
        String devicePath = "KeyRelease/MyPhoton/1";
        State releaseAction = script.state(getTestReleaseAction(domain, devicePath));

        Item restraints = script.item(Toys.Wrist_Restraints);
        assertFalse(releaseAction.applied());
        assertFalse(restraints.is(script.namespace));
        assertFalse(restraints.is(releaseAction));

        releaseAction.applyTo(restraints);
        assertTrue(releaseAction.is(script.namespace));
        assertTrue(releaseAction.is(restraints));
        assertTrue(releaseAction.applied());

        assertFalse(restraints.applied());
        assertFalse(restraints.is(script.namespace));
        assertTrue(restraints.is(releaseAction));

        restraints.apply();
        assertTrue(restraints.applied());
        assertTrue(restraints.is(script.namespace));
        assertTrue(restraints.is(releaseAction));

        assertTrue(releaseAction.applied());
        assertTrue(releaseAction.is(script.namespace));
        assertTrue(releaseAction.is(restraints));
        assertTrue(releaseAction.is(Toys.Wrist_Restraints));

        restraints.remove();
        assertFalse(restraints.applied());
        assertFalse(restraints.is(script.namespace));
        assertFalse(releaseAction.is(script.namespace));
        assertFalse(releaseAction.is(Toys.Wrist_Restraints));
        assertFalse(releaseAction.applied());

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
        assertEquals(true, (((TestReleaseActionState) ((StateProxy) releaseAction).state)).removed);
    }

    @Test
    public void testThatReleaseActionStateIsQueryable() {
        TestScript script = TestScript.getOne();

        String domain = TeaseLib.DefaultDomain;
        String devicePath = "KeyRelease/MyPhoton/1";
        State releaseAction = script.state(getTestReleaseAction(domain, devicePath));

        Item restraints = script.item(Toys.Wrist_Restraints);
        State restraintsState = script.state(Toys.Wrist_Restraints);
        assertFalse(restraints.is(ReleaseAction.class));
        assertFalse(restraintsState.is(ReleaseAction.class));

        releaseAction.applyTo(restraints);
        assertTrue(restraints.is(ReleaseAction.class));
        assertTrue(restraintsState.is(ReleaseAction.class));

        restraints.apply();
        assertTrue(restraints.is(ReleaseAction.class));
        assertTrue(restraintsState.is(ReleaseAction.class));

        restraints.remove();
        assertFalse(restraints.is(ReleaseAction.class));
        assertFalse(restraintsState.is(ReleaseAction.class));

        assertEquals(true, TestReleaseActionState.Success.getAndSet(false));
        assertEquals(true, (((TestReleaseActionState) ((StateProxy) releaseAction).state)).removed);
    }

    @Test
    public void testThatReleaseActionStateIsQueryableOnMultipleItems() {
        TestScript script = TestScript.getOne();

        String domain = TeaseLib.DefaultDomain;
        String devicePath1 = "KeyRelease/MyPhoton/1";
        String devicePath2 = "KeyRelease/MyPhoton/2";

        State removeRestraintsAction = script.teaseLib.state(domain, getTestReleaseAction(domain, devicePath1));
        State removeChainsAction = script.teaseLib.state(domain, getTestReleaseAction(domain, devicePath2));

        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        restraints.apply();
        restraints.applyTo(removeRestraintsAction);
        assertTrue(restraints.allAre(ReleaseAction.class));

        Items chains = script.items(Toys.Chains, Accessoires.Bells);
        assertFalse(chains.allAre(ReleaseAction.class));

        chains.applyTo(removeChainsAction);
        chains.applyTo(restraints);

        assertTrue(chains.allAre(ReleaseAction.class));

        Item singleChainItem = chains.get(Toys.Chains);
        singleChainItem.remove();

        assertFalse(chains.allAre(ReleaseAction.class));
        assertTrue(chains.someAre(ReleaseAction.class));

        assertFalse(chains.valueSet().stream()
                .allMatch(value -> script.teaseLib.state(TeaseLib.DefaultDomain, value).is(ReleaseAction.class)));

        Item wristRestraints = restraints.get(Toys.Wrist_Restraints);
        wristRestraints.remove();

        assertFalse(restraints.allAre(ReleaseAction.class));
        assertTrue(restraints.someAre(ReleaseAction.class));

        chains.remove();
        assertFalse(chains.allAre(ReleaseAction.class));
        assertFalse(chains.someAre(ReleaseAction.class));

        restraints.remove();
        assertFalse(restraints.allAre(ReleaseAction.class));
        assertFalse(restraints.someAre(ReleaseAction.class));
    }

    @Test
    public void ensureThatActuatorAlwaysReturnsTheSameReleaseActionInstance() {
        TestScript script = TestScript.getOne();

        TestDeviceFactory<KeyRelease> factory = new TestDeviceFactory<>("KeyRelease", script.teaseLib.devices,
                script.teaseLib.config);
        script.teaseLib.devices.get(KeyRelease.class).addFactory(factory);
        KeyRelease keyRelease = new KeyRelease(script.teaseLib.devices, factory, RemoteDevices.WaitingForConnection);

        factory.setTestDevice(keyRelease);
        Actuator a = new Actuator(keyRelease, 0);

        State releaseAction1 = script.state(a.releaseAction());
        State releaseAction2 = script.state(a.releaseAction());
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
        State release = script.state(actuator.releaseAction());

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
