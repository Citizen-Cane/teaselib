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
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class KeyReleaseScriptIntegrationTest {
    static final long requestedDurationSeconds = HOURS.toSeconds(1);

    final TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
    final Devices devices = script.teaseLib.devices;
    final DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);
    KeyRelease keyRelease;
    Actuator actuator;

    long defaultDurationSeconds;
    long availableSeconds;

    @Before
    public void before() {
        keyRelease = deviceCache.getDefaultDevice();
        KeyReleaseTest.releaseAllRunningActuators(keyRelease);

        actuator = keyRelease.actuators().available().get(1, HOURS).get();

        // TODO query default time from device - using release default time of 1h
        defaultDurationSeconds = 3600;
        availableSeconds = actuator.available(SECONDS);

        script.debugger.resumeTime();
        assertFalse(actuator.isRunning());
        assertEquals("Didn't get requested actuator", requestedDurationSeconds, availableSeconds);
        assertEquals(0, actuator.remaining(SECONDS));
    }

    @After
    public void releaseAllAfterwards() {
        KeyReleaseTest.releaseAllRunningActuators(keyRelease);
    }

    @Test
    public void testScriptEventsWithItem() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        Item restraints = cuffs.get();

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());
        script.reply("Keys placed, #title");

        script.say("Holding", Message.Delay10s);
        assertEquals("Hold duration not reset to default", defaultDurationSeconds, actuator.remaining(TimeUnit.SECONDS),
                1.0);

        script.completeAll();
        assertEquals(defaultDurationSeconds - 10, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?");
        script.reply("In a minute, #title");
        assertEquals("Hold duration not reset to default", defaultDurationSeconds, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();
        assertEquals(defaultDurationSeconds - 10, actuator.remaining(SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals(0, script.events().afterChoices.size());
        assertEquals("Release timer not reset", availableSeconds, actuator.remaining(SECONDS), 1.0);

        script.completeAll();
        assertEquals("Release timer wrong value", availableSeconds - 10, actuator.remaining(SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", availableSeconds - 20, actuator.remaining(SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(SECONDS), 1.0);
    }

    @Test
    public void testScriptEventsWithItems() {
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());
        script.reply("Keys placed, #title");

        script.say("Holding", Message.Delay10s);
        assertEquals("Hold duration not reset to default", defaultDurationSeconds, actuator.remaining(SECONDS), 1.0);

        script.completeAll();
        assertEquals(defaultDurationSeconds - 10, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?");
        script.reply("In a minute, #title");
        assertEquals("Hold duration not reset to default", defaultDurationSeconds, actuator.remaining(SECONDS), 1.0);

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();
        assertEquals(defaultDurationSeconds - 10, actuator.remaining(SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        restraints.apply();
        assertEquals(0, script.events().afterChoices.size());
        assertEquals("Release timer not reset", availableSeconds, actuator.remaining(SECONDS), 1.0);

        script.completeAll();
        assertEquals("Release timer wrong value", availableSeconds - 10, actuator.remaining(SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", availableSeconds - 20, actuator.remaining(SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);

        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(SECONDS), 1.0);
    }

    @Test
    @Ignore
    // TODO Device becomes disconnected while sending command during sleep, but reconnect fails
    public void testScriptEventsWithItemsAndSleepWhileHolding() {
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());
        script.reply("Keys placed, #title");

        // Device will not react on commands
        actuator.sleep(15, SECONDS);

        script.say("Holding", Message.Delay10s, "Are you ready?");
        script.reply("In a minute, #title");
        // Hold command fails since device is sleeping

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();
        // Woken up
        assertEquals(availableSeconds - 20, actuator.remaining(TimeUnit.SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        restraints.apply();
        assertEquals(0, script.events().afterChoices.size());
        assertEquals("Release timer not reset", availableSeconds, actuator.remaining(SECONDS), 1.0);

        actuator.sleep(15, SECONDS);
        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", availableSeconds - 20, actuator.remaining(SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);

        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(SECONDS), 1.0);
    }

}
