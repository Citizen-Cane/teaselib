package teaselib.core.devices.release;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.Message;
import teaselib.Toys;
import teaselib.core.ScriptEvents;
import teaselib.core.configuration.DebugSetup;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class KeyReleaseScriptIntegrationTest extends KeyReleaseBaseTest {
    static final long requestedDurationSeconds = HOURS.toSeconds(1);
    static final long scheduledDurationSeconds = MINUTES.toSeconds(23);

    final TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
    final ScriptEvents events = script.events();

    KeyRelease keyRelease;
    Actuator actuator;
    long availableSeconds;

    @Before
    public void before() {
        keyRelease = connectDefaultDevice(script.teaseLib.devices);
        releaseAllRunningActuators(keyRelease);

        // TODO query default time from device - for now using release default time of 1h
        actuator = keyRelease.actuators().available().get(requestedDurationSeconds, SECONDS).orElseThrow();
        availableSeconds = actuator.available(SECONDS);

        script.debugger.resumeTime();
        assertFalse(actuator.isRunning());
        assertEquals("Didn't get requested actuator", requestedDurationSeconds, availableSeconds);
        assertEquals(0, actuator.remaining(SECONDS));
    }

    @After
    public void releaseAllAfterwards() {
        releaseAllRunningActuators(keyRelease);
    }

    @Test
    public void testScriptEventsWithItem() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        Item restraints = cuffs.get();

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertArmedAndHolding();

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals("Hold renew event removed", 1, events.afterChoices.size());
        assertApplied(availableSeconds);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertEquals("Hold renew event not removed", 0, events.afterChoices.size());
        assertEquals(0, events.itemApplied.size());
        assertEquals(0, events.itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(SECONDS), 1.0);
    }

    @Test
    public void testScriptEventsWithItemOverDuration() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        Item restraints = cuffs.get();

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertArmedAndHolding();

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply().over(scheduledDurationSeconds, SECONDS);
        assertEquals("Hold renew event not removed after setting duration", 0, events.afterChoices.size());
        assertApplied(scheduledDurationSeconds);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertReleased();
    }

    @Test
    public void testScriptEventsWithItems() {
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertArmedAndHolding();

        script.say("Starting release timer", Message.Delay10s);
        restraints.apply();
        assertEquals("Hold renew event removed", 1, events.afterChoices.size());
        assertApplied(availableSeconds);

        script.say("Releasing key", Message.Delay10s);
        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased();
    }

    @Test
    public void testScriptEventsWithItemsOverDuration() {
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertArmedAndHolding();

        script.say("Starting release timer", Message.Delay10s);
        restraints.apply().over(scheduledDurationSeconds, SECONDS);
        assertEquals("Hold renew event not removed after setting duration", 0, events.afterChoices.size());
        assertApplied(scheduledDurationSeconds);

        script.say("Releasing key", Message.Delay10s);
        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased();
    }

    private void assertArmedAndHolding() {
        assertEquals(1, events.afterChoices.size());
        assertEquals(3, events.itemApplied.size());
        assertEquals(2, events.itemDuration.size());
        assertEquals(2, events.itemRemoved.size());
        script.reply("Keys placed, #title");

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
        assertTrue(actuator.isRunning());
    }

    private void assertApplied(long scheduledSeconds) {
        assertEquals("Release timer not reset", scheduledSeconds, actuator.remaining(SECONDS), 1.0);

        script.completeAll();
        assertEquals("Release timer wrong value", scheduledSeconds - 10.0, actuator.remaining(SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", scheduledSeconds - 20.0, actuator.remaining(SECONDS), 1.0);
    }

    private void assertReleased() {
        assertEquals("Hold renew event not removed", 0, events.afterChoices.size());
        assertEquals(0, events.itemApplied.size());
        assertEquals(0, events.itemDuration.size());
        assertEquals(0, events.itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(SECONDS), 1.0);
    }

    ///////////////////////////////////////

    @Test
    @Ignore
    // TODO Device becomes disconnected while sending command during sleep, but reconnect fails
    public void testScriptEventsWithItemsAndSleepWhileHolding() {
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        ScriptEvents events = script.events();
        assertEquals(1, events.afterChoices.size());
        assertEquals(3, events.itemApplied.size());
        assertEquals(2, events.itemDuration.size());
        assertEquals(3, events.itemRemoved.size());
        script.reply("Keys placed, #title");

        // Device will not react on commands
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
        restraints.apply();
        assertEquals("Hold renew event removed", 1, events.afterChoices.size());
        assertEquals("Release timer not reset", availableSeconds, actuator.remaining(SECONDS), 1.0);

        actuator.sleep(15, SECONDS);
        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", availableSeconds - 20.0, actuator.remaining(SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);

        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertReleased();
    }

}
