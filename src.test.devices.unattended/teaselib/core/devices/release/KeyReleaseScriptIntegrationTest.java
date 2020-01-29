package teaselib.core.devices.release;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.Features;
import teaselib.Message;
import teaselib.Toys;
import teaselib.core.ScriptEvents;
import teaselib.core.configuration.DebugSetup;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class KeyReleaseScriptIntegrationTest extends KeyReleaseBaseTest {
    private static final String FOOBAR = "foobar";
    static final long requestedDurationSeconds = HOURS.toSeconds(1);
    static final long scheduledDurationSeconds = MINUTES.toSeconds(23);

    final TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
    final ScriptEvents events = script.events();
    final KeyReleaseSetup keyReleaseSetup = script.script(KeyReleaseSetup.class);
    KeyRelease keyReleaseDevice;

    @Before
    public void before() throws InterruptedException {
        // TODO deviceConnectedEvent only in first test
        // - for subsequent tests, no startup message is received
        // -> LocalNetworkDevice returns no devices
        awaitConnection(script.teaseLib.devices.get(KeyRelease.class));
        keyReleaseSetup.say(FOOBAR);

        keyReleaseDevice = getDefaultDevice();
        releaseAllRunningActuators(keyReleaseDevice);

        script.debugger.resumeTime();
    }

    @After
    public void releaseActuators() {
        releaseAllRunningActuators(keyReleaseDevice);
    }

    private long availableSeconds(Items items) {
        return keyReleaseSetup.getActuator(items).orElseThrow().available(SECONDS);
    }

    @Test
    public void testScriptEventsWithItem() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Coupled);
        long availableSeconds = availableSeconds(cuffs);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        keyReleaseSetup.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals("Hold renew event removed", 1, events.afterChoices.size());
        assertApplied(cuffs, availableSeconds);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertReleased(cuffs);
    }

    @Test
    public void testScriptEventsWithItemOverDuration() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Coupled);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        keyReleaseSetup.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply().over(scheduledDurationSeconds, SECONDS);
        assertEquals("Hold renew event not removed after setting duration", 0, events.afterChoices.size());
        assertApplied(cuffs, scheduledDurationSeconds);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertReleased(cuffs);
    }

    @Test
    public void testScriptEventsWithItems() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Coupled);
        long availableSeconds = availableSeconds(cuffs);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        keyReleaseSetup.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals("Hold renew event removed", 1, events.afterChoices.size());
        assertApplied(cuffs, availableSeconds);

        script.say("Releasing key", Message.Delay10s);
        Item wristCuffs = cuffs.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased(cuffs);
    }

    @Test
    public void testScriptEventsWithItemsOverDuration() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints).matching(Features.Coupled);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        keyReleaseSetup.say(FOOBAR);
        assertArmedAndHolding(cuffs);

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply().over(scheduledDurationSeconds, SECONDS);
        assertEquals("Hold renew event not removed after setting duration", 0, events.afterChoices.size());
        assertApplied(cuffs, scheduledDurationSeconds);

        script.say("Releasing key", Message.Delay10s);
        Item wristCuffs = cuffs.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased(cuffs);
    }

    private void assertArmedAndHolding(Items items) {
        assertEquals(1, events.afterChoices.size());
        assertEquals(keyReleaseDevice.actuators().size(), events.itemApplied.size());
        assertEquals(0, events.itemDuration.size());
        assertEquals(0, events.itemRemoved.size());
        script.reply("Keys placed, #title");

        Actuator actuator = keyReleaseSetup.getActuator(items.get()).orElseThrow();
        assertTrue(actuator.isRunning());

        script.say("Holding", Message.Delay10s);
        assertEquals("Hold duration not reset to default", requestedDurationSeconds, actuator.remaining(SECONDS), 1.0);

        script.completeAll();
        assertEquals(requestedDurationSeconds - 10.0, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?");
        script.reply("In a minute, #title");
        assertEquals("Hold duration not reset to default", requestedDurationSeconds, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();
        assertEquals(requestedDurationSeconds - 10.0, actuator.remaining(SECONDS), 1.0);
    }

    private void assertApplied(Items items, long scheduledSeconds) {
        Actuator actuator = keyReleaseSetup.getActuator(items).orElseThrow();
        assertTrue(actuator.isRunning());
        assertEquals("Release timer not reset", scheduledSeconds, actuator.remaining(SECONDS), 1.0);

        script.completeAll();
        assertEquals("Release timer wrong value", scheduledSeconds - 10.0, actuator.remaining(SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", scheduledSeconds - 20.0, actuator.remaining(SECONDS), 1.0);
    }

    private void assertReleased(Items items) {
        Actuator actuator = keyReleaseSetup.getActuator(items).orElseThrow();
        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(SECONDS), 1.0);

        assertEquals("Hold renew event not removed", 0, events.afterChoices.size());
        assertEquals(keyReleaseDevice.actuators().size(), events.itemApplied.size());
        assertEquals(0, events.itemDuration.size());
        assertEquals(0, events.itemRemoved.size());

    }

    ///////////////////////////////////////

    @Test
    @Ignore
    // TODO Device becomes disconnected while sending command during sleep, but reconnect fails
    public void testScriptEventsWithItemsAndSleepWhileHolding() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        Actuator actuator = keyReleaseSetup.getActuator(cuffs).orElseThrow();
        long availableSeconds = actuator.available(TimeUnit.SECONDS);

        script.say("Arm", Message.Delay10s);
        keyReleaseSetup.prepare(cuffs, 1, TimeUnit.HOURS, script::show);
        assertEquals(1, events.afterChoices.size());
        assertEquals(3, events.itemApplied.size());
        assertEquals(2, events.itemDuration.size());
        assertEquals(3, events.itemRemoved.size());
        script.reply("Keys placed, #title");

        // Device will not react on commands afterwards
        actuator.sleep(15, SECONDS);

        script.say("Holding", Message.Delay10s, "Are you ready?");
        script.reply("In a minute, #title");
        // Hold command fails since device is sleeping

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();

        // Woken up
        assertEquals(availableSeconds - 20.0, actuator.remaining(TimeUnit.SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals("Hold renew event removed", 1, events.afterChoices.size());
        assertEquals("Release timer not reset", availableSeconds, actuator.remaining(SECONDS), 1.0);

        actuator.sleep(15, SECONDS);
        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", availableSeconds - 20.0, actuator.remaining(SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);

        Item wristCuffs = cuffs.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased(cuffs);
    }

}
