package teaselib.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import teaselib.Features;
import teaselib.Toys;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.release.KeyReleaseBaseTest;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class ScriptEventsTest extends KeyReleaseBaseTest {
    private TestScript script;
    private KeyReleaseSetup keyReleaseSetup;
    private KeyRelease keyRelease;

    @Before
    public void setupActuators() {
        script = TestScript.getOne(new DebugSetup());
        keyReleaseSetup = script.script(KeyReleaseSetup.class);
        keyRelease = new KeyReleaseMock(
                Arrays.asList(new ActuatorMock(2, TimeUnit.HOURS), new ActuatorMock(1, TimeUnit.HOURS)));
        keyReleaseSetup.deviceConnected(new DeviceEventMock(keyRelease));

        assertEquals(2, keyReleaseSetup.itemDurationSeconds.size());
        assertEquals(2, keyReleaseSetup.itemActuators.size());
        assertEquals(2, keyReleaseSetup.actuatorItems.size());
    }

    @After
    public void detachDevice() {
        keyReleaseSetup.deviceDisconnected(new DeviceEventMock(keyRelease));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeyReleaseEventHandlingPrepareWithoutSelect() {
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        Items cuffs = items.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints);
        script.script(KeyReleaseSetup.class).prepare(cuffs);
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveAllItems() {
        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled);
        keyReleaseSetup.prepare(cuffs);
        assertArmedAndHolding();

        // TODO the items collection contains all elements, usually selects one
        // -> leather restraints and cuffs are applied
        // -> resolve by matching detachable (leather) or coupled (handcuffs) items Toys.Ankle_Restraints,
        // Toys.Wrist_Restraints
        // TODO matching break generic scripts that just apply
        cuffs.apply();
        // TODO generic items Toys.Ankle_Restraints, Toys.Wrist_Restraints are applied one after another
        // -> triggers both actuators one after another
        assertApplied();

        cuffs.remove();
        assertIdle();
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveOneItem() {
        Items items = new Items(
                script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Detachable), //
                script.items(Toys.Collar) //
        );

        keyReleaseSetup.prepare(items);
        assertArmedAndHolding();

        items.apply();
        assertApplied();

        items.get(Toys.Wrist_Restraints).remove();
        assertIdle();
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveFromPeers() {
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        keyReleaseSetup.prepare(new Items(chains));
        assertArmedAndHolding();

        chains.applyTo(items);
        assertApplied();

        chains.removeFrom(items);
        assertIdle();
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveFromOnePeer() {
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        keyReleaseSetup.prepare(new Items(chains));
        assertArmedAndHolding();

        chains.applyTo(items);
        assertApplied();

        chains.removeFrom(items.get(Toys.Collar));
        assertIdle();
    }

    @Test
    public void testKeyReleaseEventHandlingApplyBeforeRemoveOnly() {
        Items cuffs = new Items(
                script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Detachable), //
                script.items(Toys.Humbler, Toys.Collar) //
        );

        cuffs.get(Toys.Collar).apply(); // before, so this doesn't start the timer
        assertIdle();

        keyReleaseSetup.prepare(cuffs);
        assertArmedAndHolding();

        cuffs.get(Toys.Humbler).apply();
        cuffs.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).apply();

        assertApplied();

        cuffs.items(Toys.Collar).remove();
        cuffs.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).remove();
        assertIdle();
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveAllItems() {
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        assertIdle();

        items.apply();
        assertApplied();

        items.remove();
        assertIdle();
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveOneItem() {
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        assertIdle();

        items.apply();
        assertApplied();

        items.get(Toys.Wrist_Restraints).remove();
        assertIdle();
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveFromPeers() {
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        assertIdle();

        chains.applyTo(items);
        assertApplied();

        chains.removeFrom(items);
        assertIdle();
    }

    @Test
    public void testKeyReleaseDefaultHandlerFromOnePeer() {
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        assertIdle();

        chains.applyTo(items);
        assertApplied();

        chains.removeFrom(items.get(Toys.Collar));
        assertIdle();
    }

    @Test
    public void testKeyReleaseDefaultHandlerApplyBeforeRemoveOnly() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Humbler, Toys.Collar);

        cuffs.get(Toys.Collar).apply(); // before, so this doesn't start the timer
        assertIdle();

        cuffs.get(Toys.Humbler).apply();
        cuffs.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).apply();
        assertApplied();

        cuffs.items(Toys.Collar).remove();
        cuffs.items(Toys.Humbler).remove();
        cuffs.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).remove();
        assertIdle();
    }

    private void assertIdle() {
        assertEquals(0, script.events().afterChoices.size());
        assertEquals("Expected all apply hook active", keyRelease.actuators().size(),
                script.events().itemApplied.size());
        assertEquals(0, script.events().itemDuration.size());
        assertEquals(0, script.events().itemRemoved.size());
    }

    private void assertArmedAndHolding() {
        assertEquals(1, script.events().afterChoices.size());
        assertEquals("Expected all apply hook active", keyRelease.actuators().size(),
                script.events().itemApplied.size());
        assertEquals(0, script.events().itemDuration.size());
        assertEquals(0, script.events().itemRemoved.size());
    }

    private void assertApplied() {
        assertEquals(1, script.events().afterChoices.size());
        assertEquals("Expected apply hook removed", keyRelease.actuators().size() - 1L,
                script.events().itemApplied.size());
        assertEquals(1, script.events().itemDuration.size());
        assertEquals(1, script.events().itemRemoved.size());
    }

}
