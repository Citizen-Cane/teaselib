package teaselib.core.devices.release.unattended;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.Bondage;
import teaselib.Features;
import teaselib.Message;
import teaselib.State.Persistence.Until;
import teaselib.core.ScriptEvents;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.devices.release.KeyReleaseBaseTest;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class KeyReleaseScriptIntegrationTest extends KeyReleaseBaseTest {

    private static final String FOOBAR = "foobar";
    static final long requestedDurationSeconds = HOURS.toSeconds(1);
    static final long scheduledDurationSeconds = MINUTES.toSeconds(23);

    TestScript script;
    ScriptEvents events;
    KeyReleaseSetup keyReleaseSetup;
    KeyRelease keyReleaseDevice;

    @Before
    public void before() throws InterruptedException, IOException {
        script = new TestScript(new DebugSetup().withRemoteDeviceAccess());
        events = script.events();
        keyReleaseSetup = script.interaction(KeyReleaseSetup.class);

        // TODO deviceConnectedEvent only in first test
        // - for subsequent tests, no startup message is received
        // -> LocalNetworkDevice returns no devices
        awaitConnection(script.teaseLib.devices.get(KeyRelease.class));
        script.say(FOOBAR);

        keyReleaseDevice = script.teaseLib.devices.getDefaultDevice(KeyRelease.class);
        assertFalse("No Key-Release Device found", keyReleaseDevice.actuators().isEmpty());

        releaseAllRunningActuators(keyReleaseDevice);
        script.debugger.resumeTime();
    }

    @After
    public void releaseActuators() {
        releaseAllRunningActuators(keyReleaseDevice);
        script.close();
    }

    private long availableSeconds(Items items) {
        return keyReleaseSetup.deviceInteraction.getActuator(items).orElseThrow().available(SECONDS);
    }

    @Test
    public void testScriptEventsWithItem() {
        Items cuffs = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).without(Features.Detachable)
                .inventory();
        long availableSeconds = availableSeconds(cuffs);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals("Hold renew event removed", 1, events.afterPrompt.size());
        assertApplied(cuffs, availableSeconds);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertReleased(cuffs);
    }

    @Test
    public void testScriptEventsWithItemOverDuration() {
        Items cuffs = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).without(Features.Detachable)
                .inventory();

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply().over(scheduledDurationSeconds, SECONDS).remember(Until.Expired);
        assertEquals("Hold renew event not removed after setting duration", 0, events.afterPrompt.size());
        assertApplied(cuffs, scheduledDurationSeconds);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertReleased(cuffs);
    }

    @Test
    public void testScriptEventsWithItems() {
        script.setAvailable(Bondage.All);
        var cuffs = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).without(Features.Detachable)
                .getApplicableSet();
        long availableSeconds = availableSeconds(cuffs);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals("Hold renew event removed", 1, events.afterPrompt.size());
        assertApplied(cuffs, availableSeconds);

        script.say("Releasing key", Message.Delay10s);
        Item wristCuffs = cuffs.get(Bondage.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased(cuffs);
    }

    @Test
    public void testScriptEventsWithItemsOverDuration() {
        script.setAvailable(Bondage.All);
        var cuffs = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).without(Features.Detachable)
                .getApplicableSet();

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        script.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply().over(scheduledDurationSeconds, SECONDS).remember(Until.Expired);
        assertEquals("Hold renew event not removed after setting duration", 0, events.afterPrompt.size());
        assertApplied(cuffs, scheduledDurationSeconds);

        script.say("Releasing key", Message.Delay10s);
        Item wristCuffs = cuffs.get(Bondage.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased(cuffs);
    }

    private void assertArmedAndHolding(Items items) {
        assertEquals(1, events.afterPrompt.size());
        assertEquals(keyReleaseDevice.actuators().size(), events.itemApplied.size());
        assertEquals(0, events.itemRemember.size());
        assertEquals(0, events.itemRemoved.size());
        script.reply("Keys placed, #title");

        Actuator actuator = keyReleaseSetup.deviceInteraction.getActuator(items.get(0)).orElseThrow();
        assertTrue(actuator.isRunning());

        script.say("Holding", Message.Delay10s);
        assertEquals("Hold duration not reset to default", requestedDurationSeconds, actuator.remaining(SECONDS), 1.0);

        script.awaitAllCompleted();
        assertEquals(requestedDurationSeconds - 10.0, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?");
        script.reply("In a minute, #title");
        assertEquals("Hold duration not reset to default", requestedDurationSeconds, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?", Message.Delay10s);
        script.awaitAllCompleted();
        assertEquals(requestedDurationSeconds - 10.0, actuator.remaining(SECONDS), 1.0);
    }

    private void assertApplied(Items items, long scheduledSeconds) {
        Actuator actuator = keyReleaseSetup.deviceInteraction.getActuator(items).orElseThrow();
        assertTrue(actuator.isRunning());
        assertEquals("Release timer not reset", scheduledSeconds, actuator.remaining(SECONDS), 1.0);

        script.awaitAllCompleted();
        assertEquals("Release timer wrong value", scheduledSeconds - 10.0, actuator.remaining(SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.awaitAllCompleted();
        assertEquals("Release timer wrong value", scheduledSeconds - 20.0, actuator.remaining(SECONDS), 1.0);
    }

    private void assertReleased(Items items) {
        Actuator actuator = keyReleaseSetup.deviceInteraction.getActuator(items).orElseThrow();
        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(SECONDS), 1.0);

        assertEquals("Hold renew event not removed", 0, events.afterPrompt.size());
        assertEquals(keyReleaseDevice.actuators().size(), events.itemApplied.size());
        assertEquals(0, events.itemRemember.size());
        assertEquals(0, events.itemRemoved.size());

    }

    ///////////////////////////////////////

    @Test
    @Ignore
    // TODO Device becomes disconnected while sending command during sleep, but reconnect fails
    public void testScriptEventsWithItemsAndSleepWhileHolding() {
        script.setAvailable(Bondage.All);
        var cuffs = script.items(Bondage.Wrist_Restraints, Bondage.Ankle_Restraints).without(Features.Detachable)
                .getApplicableSet();
        Actuator actuator = keyReleaseSetup.deviceInteraction.getActuator(cuffs).orElseThrow();
        long availableSeconds = actuator.available(TimeUnit.SECONDS);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        assertEquals(1, events.afterPrompt.size());
        assertEquals(3, events.itemApplied.size());
        assertEquals(2, events.itemRemember.size());
        assertEquals(3, events.itemRemoved.size());
        script.reply("Keys placed, #title");

        // Device will not react on commands afterwards
        actuator.sleep(15, SECONDS);

        script.say("Holding", Message.Delay10s, "Are you ready?");
        script.reply("In a minute, #title");
        // Hold command fails since device is sleeping

        script.say("Are you ready?", Message.Delay10s);
        script.awaitAllCompleted();

        // Woken up
        assertEquals(availableSeconds - 20.0, actuator.remaining(TimeUnit.SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals("Hold renew event removed", 1, events.afterPrompt.size());
        assertEquals("Release timer not reset", availableSeconds, actuator.remaining(SECONDS), 1.0);

        actuator.sleep(15, SECONDS);
        script.say("Timer is running", Message.Delay10s);
        script.awaitAllCompleted();
        assertEquals("Release timer wrong value", availableSeconds - 20.0, actuator.remaining(SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);

        Item wristCuffs = cuffs.get(Bondage.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased(cuffs);
    }

}
