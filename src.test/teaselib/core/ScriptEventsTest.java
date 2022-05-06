package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import teaselib.Bondage;
import teaselib.Features;
import teaselib.State;
import teaselib.State.Options;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.release.KeyReleaseBaseTest;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.core.state.ItemProxy;
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
    public void setupActuators() throws IOException {
        script = new TestScript();
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
        actuatorMocks.stream().forEach(Actuator::release);
        keyReleaseSetup.deviceInteraction.deviceDisconnected(new DeviceEventMock(keyRelease));
        script.close();
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

        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled).inventory();
        assertFalse(keyReleaseSetup.isPrepared(cuffs));
        assertTrue(keyReleaseSetup.canPrepare(cuffs));

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        assertFalse("Actuator mock not released after previous test", keyReleaseSetup.isPrepared(cuffs));
        assertTrue(keyReleaseSetup.canPrepare(cuffs));

        AtomicBoolean instructionsCalled = new AtomicBoolean(false);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, items -> instructionsCalled.set(true));

        script.say(FOOBAR);
        assertHoldActions(1);
        assertApplyActions(2);
        assertTrue(instructionsCalled.get());

        cuffs.apply();
        script.say(FOOBAR);

        assertApplyActions(1);
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
    public void testKeyReleaseEventHandlingRemoveOneItem() {
        simulateDeviceConnect();

        var itemsReplacingAllDefaults = script.items(script.items(Bondage.Wristlets, Bondage.Anklets).inventory(), //
                script.items(Toys.Collar).inventory() //
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

        itemsReplacingAllDefaults.items(Bondage.Wristlets).remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveFromPeers() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar).inventory();
        Item chains = script.item(Bondage.Chains);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        keyReleaseSetup.prepare(script.items(chains), 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        chains.to(items).apply();
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

        script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar).inventory()
                .forEach(item -> item.setAvailable(true));
        var items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar).getApplicableSet();
        Item chainsReplaceOneDefault = script.item(Bondage.Chains);

        keyReleaseSetup.prepare(script.items(chainsReplaceOneDefault), 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        chainsReplaceOneDefault.to(items).apply();
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
    public void testKeyReleaseEventHandlingInstructionsAgain() {
        simulateDeviceConnect();

        script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar).inventory()
                .forEach(item -> item.setAvailable(true));
        var items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar).getApplicableSet();
        Item chainsReplaceOneDefault = script.item(Bondage.Chains);

        keyReleaseSetup.prepare(script.items(chainsReplaceOneDefault), 1, TimeUnit.HOURS, script::show, script::show);
        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);

        chainsReplaceOneDefault.to(items).apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        chainsReplaceOneDefault.removeFrom(items.get(Toys.Collar));
        assertApplyActions(2);
        assertHoldActions(1);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseEventHandlingApplyBeforePrepareRemoveSingleItem() {
        simulateDeviceConnect();

        script.setAvailable(Toys.All, Bondage.All);
        var items = script.items(script.items(Bondage.Wristlets, Bondage.Anklets), //
                script.items(Toys.Humbler, Toys.Collar) //
        ).getApplicableSet();

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        Item collarAppliedBeforePrepare = items.get(Toys.Collar);
        collarAppliedBeforePrepare.apply();
        assertApplyActions(2);

        // TODO Do detachable items release the coupled cuffs?
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

        Items items = script.items(Bondage.Wristlets, Bondage.Anklets, Toys.Collar).inventory();

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

        Items items = script.items(Bondage.Wristlets, Bondage.Anklets, Toys.Collar).inventory();

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

        options.over(1, TimeUnit.HOURS).remember(Until.Expired);
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

        Items cuffsFirst = script.items(Bondage.Wristlets, Bondage.Anklets).inventory();
        Items chainsSecond = script.items(Bondage.Chains).inventory();

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

        applied.over(1, TimeUnit.HOURS).remember(Until.Expired);
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

        var items = script.items(Bondage.Wristlets, Bondage.Anklets, Toys.Collar).inventory();

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

        items.items(Bondage.Wristlets).remove();
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseDefaultHandlerRemoveFromPeers() {
        simulateDeviceConnect();

        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar).inventory();
        Item chains = script.item(Bondage.Chains);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        chains.to(items).apply();
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

        script.setAvailable(Toys.All, Bondage.All);
        var items = script.items(Bondage.Wristlets, Bondage.Anklets, Toys.Collar).getApplicableSet();
        Item chains = script.item(Bondage.Chains);

        script.say(FOOBAR);
        assertApplyActions(2);

        chains.to(items).apply();
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

        script.setAvailable(Toys.All, Bondage.All);
        Items.Set cuffs = script.items(Bondage.Wristlets, Bondage.Anklets, Toys.Humbler).getApplicableSet();

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        cuffs.get(Toys.Humbler).apply();
        cuffs.items(Bondage.Wristlets, Bondage.Anklets).apply();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffs.items(Toys.Humbler).remove();
        assertApplyActions(1);
        assertHoldActions(1);
        assertCountdownActions(1);
        assertRemoveActions(1);

        cuffs.items(Bondage.Wristlets, Bondage.Anklets).remove();

        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);
    }

    @Test
    public void testKeyReleaseConnectAfterPrepare() {
        Items items = script.items(
                script.items(Bondage.Wristlets, Bondage.Anklets).prefer(Features.Lockable).inventory(),
                script.items(Toys.Collar).inventory());

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
        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled).inventory();
        assertFalse(keyReleaseSetup.isPrepared(cuffs));
        assertTrue(keyReleaseSetup.canPrepare(cuffs));

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
        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled).inventory();

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
        Items cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled).inventory();
        assertFalse(keyReleaseSetup.isPrepared(cuffs));
        assertTrue(keyReleaseSetup.canPrepare(cuffs));

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

        script.setAvailable(Toys.All);
        Items.Set cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled)
                .getApplicableSet();
        assertFalse(keyReleaseSetup.isPrepared(cuffs));
        assertTrue(keyReleaseSetup.canPrepare(cuffs));

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

    @Test
    public void testPrepareChangeInstructions() {
        simulateDeviceConnect();

        script.setAvailable(Toys.All);
        var cuffs = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints).matching(Features.Coupled)
                .getApplicableSet();
        assertTrue("ItemProxy instances expected for event counting",
                cuffs.stream().allMatch(ItemProxy.class::isInstance));

        AtomicBoolean instructionsCalled = new AtomicBoolean(false);
        AtomicBoolean instructionsCalledAgain = new AtomicBoolean(false);

        assertTrue(keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, //
                items -> instructionsCalled.set(true), items -> instructionsCalledAgain.set(true)));
        assertTrue(keyReleaseSetup.isPrepared(cuffs));
        assertTrue(instructionsCalled.get());
        assertFalse(instructionsCalledAgain.get());

        AtomicBoolean instructionsCalled2 = new AtomicBoolean(false);
        AtomicBoolean instructionsCalledAgain2 = new AtomicBoolean(false);
        assertFalse(keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, //
                items -> instructionsCalled2.set(true), items -> instructionsCalledAgain2.set(true)));
        assertTrue(keyReleaseSetup.isPrepared(cuffs));
        assertFalse(instructionsCalled2.get());
        assertFalse(instructionsCalledAgain2.get());

        instructionsCalled.set(false);

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

        cuffs.get(Toys.Ankle_Restraints).remove();

        assertApplyActions(2);
        assertHoldActions(1); // because of instructionsAgain
        assertCountdownActions(0);
        assertRemoveActions(0);

        assertFalse(instructionsCalled.get());
        assertFalse(instructionsCalledAgain.get());
        assertFalse(instructionsCalled2.get());
        assertTrue(instructionsCalledAgain2.get());
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
        assertEquals("Expected actuator to be prepared and holding", count, script.events().afterPrompt.size());
    }

    private void assertCountdownActions(int count) {
        assertEquals(count, script.events().itemRemember.size());
    }

    private void assertRemoveActions(int count) {
        assertEquals(count, script.events().itemRemoved.size());
    }

}
