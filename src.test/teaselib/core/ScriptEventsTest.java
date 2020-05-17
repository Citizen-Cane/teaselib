package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        keyReleaseSetup = script.interaction(KeyReleaseSetup.class);
        keyRelease = new KeyReleaseMock(actuatorMocks);
        keyReleaseSetup.deviceConnected(new DeviceEventMock(keyRelease));

        assertEquals(2, keyReleaseSetup.itemDurationSeconds.size());
        assertEquals(0, keyReleaseSetup.itemActuators.size());
        assertEquals(0, keyReleaseSetup.actuatorItems.size());
    }

    @After
    public void detachDevice() {
        keyReleaseSetup.deviceDisconnected(new DeviceEventMock(keyRelease));
    }

    @Test
    public void testPreparingRemovesDefaults() {
        Items appearingInMultipleDefaults = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints);
        assertEquals(2, keyReleaseSetup.itemDurationSeconds.size());

        keyReleaseSetup.prepare(appearingInMultipleDefaults, 1, TimeUnit.HOURS, script::show);
        assertEquals(1, keyReleaseSetup.itemDurationSeconds.size());
    }

    @Test
    public void testDeviceConntedIsForwardedToScriptThread() {
        assertApplyActions(0);
        script.say(FOOBAR);
        assertApplyActions(2);
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveAllItems() {
        Items cuffsReplaceSingleDefaultPreparation = script.items(Toys.Ankle_Restraints, Toys.Wrist_Restraints)
                .matching(Features.Coupled);

        script.say(FOOBAR);
        assertApplyActions(2);
        assertHoldActions(0);
        assertCountdownActions(0);
        assertRemoveActions(0);

        assertTrue(keyReleaseSetup.canPrepare(cuffsReplaceSingleDefaultPreparation));
        keyReleaseSetup.prepare(cuffsReplaceSingleDefaultPreparation, 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertHoldActions(1);
        assertApplyActions(2);

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

    private void assertApplyActions(int count) {
        assertEquals("Expected all apply-hooks active", count, script.events().itemApplied.size());
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
