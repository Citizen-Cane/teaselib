package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import teaselib.Features;
import teaselib.State;
import teaselib.State.Options;
import teaselib.Toys;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.release.KeyReleaseBaseTest;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class ScriptEventsTest extends KeyReleaseBaseTest {
    private static final List<Actuator> actuatorMocks = Arrays.asList(new ActuatorMock(2, TimeUnit.HOURS),
            new ActuatorMock(1, TimeUnit.HOURS));

    static final String FOOBAR = "foobar";

    private TestScript script;
    private KeyReleaseSetup keyReleaseSetup;
    private KeyRelease keyRelease;

    @Before
    public void setupActuators() {
        script = TestScript.getOne(new DebugSetup());
        script.debugger.freezeTime();
        keyReleaseSetup = script.interaction(KeyReleaseSetup.class);
        keyRelease = new KeyReleaseMock(actuatorMocks);
    }

    private void simulateDeviceConnect() {
        keyReleaseSetup.deviceInteraction.deviceConnected(new DeviceEventMock(keyRelease));
        assertEquals(2, keyRelease.actuators().size());
        assertEquals(2, keyReleaseSetup.deviceInteraction.definitions(script.actor).size());
        assertTrue(keyReleaseSetup.deviceAvailable());
    }

    @After
    public void detachDevice() {
        keyReleaseSetup.deviceInteraction.deviceDisconnected(new DeviceEventMock(keyRelease));
    }

    @Test
    public void testPreparingRemovesDefaults() {
        Items appearingInMultipleDefaults = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints);
        assertEquals(2, keyReleaseSetup.deviceInteraction.definitions(script.actor).size());

        keyReleaseSetup.prepare(appearingInMultipleDefaults, 1, TimeUnit.HOURS, script::show);
        assertEquals(1, keyReleaseSetup.deviceInteraction.definitions(script.actor).size());
    }

    @Test
    public void testDeviceConntedIsForwardedToScriptThread() {
        simulateDeviceConnect();

        assertApplyActions(0);
        script.say(FOOBAR);
        assertApplyActions(2);
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveAllItems() {
        simulateDeviceConnect();

        Items cuffsReplaceSingleDefaultPreparation = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints)
                .matching(Features.Coupled);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        assertTrue(keyReleaseSetup.canPrepare(cuffsReplaceSingleDefaultPreparation));
        AtomicBoolean instructionsCalled = new AtomicBoolean(false);
        keyReleaseSetup.prepare(cuffsReplaceSingleDefaultPreparation, 1, TimeUnit.HOURS,
                items -> instructionsCalled.set(true));

        script.say(FOOBAR);
        assertHoldActions(1);
        assertApplyActions(2);
        assertTrue(instructionsCalled.get());

        // TODO the items collection contains all elements, usually selects one
        // -> leather restraints and cuffs are applied
        // -> resolve by matching detachable (leather) or coupled (handcuffs) items Toys.Ankle_Restraints,
        // Toys.Wrist_Restraints
        // TODO matching breaks generic scripts that just apply
        cuffsReplaceSingleDefaultPreparation.apply();
        script.say(FOOBAR);

        // TODO generic items Toys.Ankle_Restraints, Toys.Wrist_Restraints are applied one after another
        // -> triggers both actuators one after another
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffsReplaceSingleDefaultPreparation.remove();
        script.say(FOOBAR);

        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveOneItem() {
        simulateDeviceConnect();

        Items itemsReplacingAllDefaults = new Items(
                script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Detachable), //
                script.items(Toys.Collar) //
        );

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        keyReleaseSetup.prepare(itemsReplacingAllDefaults, 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        itemsReplacingAllDefaults.apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        itemsReplacingAllDefaults.get(Toys.Wrist_Restraints).remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveFromPeers() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        keyReleaseSetup.prepare(new Items(chains), 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        chains.applyTo(items);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        chains.removeFrom(items);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingReplaceOneDefaultRemoveFromOnePeer() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chainsReplaceOneDefault = script.item(Toys.Chains);

        keyReleaseSetup.prepare(new Items(chainsReplaceOneDefault), 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        chainsReplaceOneDefault.applyTo(items);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        chainsReplaceOneDefault.removeFrom(items.get(Toys.Collar));
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingApplyBeforePrepareRemoveSingleItem() {
        simulateDeviceConnect();

        Items items = new Items(
                script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Detachable), //
                script.items(Toys.Humbler, Toys.Collar) //
        );

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        Item collarAppliedBeforePrepare = items.get(Toys.Collar);
        collarAppliedBeforePrepare.apply();
        assertApplyActions(2);

        // TODO Do unlockable items remove the second key items category
        keyReleaseSetup.prepare(items, 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        // TODO lock two toys categories to different actuators
        Item humbler = items.get(Toys.Humbler);
        humbler.apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        Items moreItemsOnSameActuator = items.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        moreItemsOnSameActuator.apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        items.items(Toys.Collar).remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        moreItemsOnSameActuator.remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        humbler.remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveAllItems() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        items.apply();
        script.say(FOOBAR);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        items.remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerCountdownRemoveAllItems() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        Options options = items.apply();
        script.say(FOOBAR);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        options.over(1, TimeUnit.HOURS);
        assertApplyActions(1);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(1);

        items.remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerTwoActuatorsRemoveAllItems() {
        simulateDeviceConnect();

        Items cuffsFirst = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Detachable);
        Items chainsSecond = script.items(Toys.Chains);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        cuffsFirst.apply();
        script.say(FOOBAR);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        State.Options applied = chainsSecond.apply();
        script.say(FOOBAR);
        assertApplyActions(0);
        assertHoldActions(2);
        assertCountdownActions(2);
        assertRemoveActions(2);

        applied.over(1, TimeUnit.HOURS);
        assertApplyActions(0);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(2);

        chainsSecond.remove();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffsFirst.remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveOneItem() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        items.apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        items.get(Toys.Wrist_Restraints).remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveFromPeers() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        chains.applyTo(items);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        chains.removeFrom(items);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerFromOnePeer() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        script.say(FOOBAR);
        assertApplyActions(2);

        chains.applyTo(items);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        chains.removeFrom(items.get(Toys.Collar));
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveUnlockableItems() {
        simulateDeviceConnect();

        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Humbler);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        cuffs.get(Toys.Humbler).apply();
        cuffs.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffs.items(Toys.Humbler).remove();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffs.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).remove();

        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseConnectAfterPrepare() {
        Items items = new Items(script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).prefer(Features.Lockable),
                script.items(Toys.Collar));

        AtomicBoolean instructionsCalled = new AtomicBoolean(false);
        keyReleaseSetup.prepare(items, 1, TimeUnit.HOURS, cuffs -> instructionsCalled.set(true));
        assertFalse(instructionsCalled.get());

        script.say(FOOBAR);
        assertFalse(instructionsCalled.get());
        assertApplyActions(0);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        simulateDeviceConnect();
        script.say(FOOBAR);
        assertTrue(instructionsCalled.get());

        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        items.apply();
        script.say(FOOBAR);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        items.remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingConnectAfterApplyAssignsActuator() {
        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled);

        script.say(FOOBAR);
        assertApplyActions(0);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        cuffs.apply();
        simulateDeviceConnect();
        script.say(FOOBAR);

        assertApplyActions(1);
        assertArmedActions(1);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        script.say(FOOBAR);

        assertApplyActions(1);
        assertArmedActions(0);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffs.remove();
        script.say(FOOBAR);

        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingConnectAfterApplyAssignsActuatorOverDuration() {
        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled);

        script.say(FOOBAR);
        assertApplyActions(0);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        cuffs.apply().over(30, TimeUnit.MINUTES);
        simulateDeviceConnect();
        script.say(FOOBAR);

        assertApplyActions(1);
        assertArmedActions(1);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        script.say(FOOBAR); // transition from arm -> start-> count down

        assertApplyActions(1);
        assertArmedActions(0);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertEquals(1800, keyRelease.actuators().get(1).remaining(TimeUnit.SECONDS));
        assertRemoveActions(1);

        cuffs.remove();
        script.say(FOOBAR);

        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingConnectAfterApplyAssignsActuatorWithPreparation() {
        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled);

        AtomicBoolean instructionsCalled = new AtomicBoolean(false);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, items -> instructionsCalled.set(true));

        script.say(FOOBAR);
        assertApplyActions(0);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        cuffs.apply();
        assertFalse(instructionsCalled.get());

        simulateDeviceConnect();
        script.say(FOOBAR); // transition idle -> Arm
        assertTrue(instructionsCalled.get());

        assertApplyActions(1);
        assertArmedActions(1);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        script.say(FOOBAR); // transition Arm -> Hold

        assertApplyActions(1);
        assertArmedActions(0);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffs.remove();
        script.say(FOOBAR);

        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingClearPreparationBeforeRemove() {
        simulateDeviceConnect();

        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled);

        AtomicBoolean instructionsCalled = new AtomicBoolean(false);
        AtomicBoolean instructionsCalledAgain = new AtomicBoolean(false);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, //
                items -> instructionsCalled.set(true), items -> instructionsCalledAgain.set(true));
        assertTrue(keyReleaseSetup.isPrepared(cuffs));
        assertTrue(instructionsCalled.get());

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        cuffs.apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        assertTrue(keyReleaseSetup.isPrepared(cuffs));
        keyReleaseSetup.clear(cuffs);
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffs.get(Toys.Ankle_Restraints).remove();

        assertApplyActions(1); // one apply hook less after clearing preparation
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        assertFalse(instructionsCalledAgain.get());
        assertFalse(keyReleaseSetup.isPrepared(cuffs));
    }

    // TODO passing keys between actors
    // TODO test that clearing preparation before apply should releases key

    private void assertApplyActions(int count) {
        assertEquals("Expected all apply-hooks active", count, script.events().itemApplied.size());
    }

    private void assertArmedActions(int count) {
        assertEquals("Expected actuator to be prepared and armed", count, script.events().beforeMessage.size());
    }

    private void assertHoldActions(int count) {
        assertEquals("Expected actuator to be prepared and holding", count, script.events().afterChoices.size());
    }

    private void assertCountdownActions(int count) {
        assertEquals(count, script.events().itemDuration.size());
    }

    private void assertRemoveActions(int count) {
        assertEquals(count, script.events().itemRemoved.size());
    }

}
