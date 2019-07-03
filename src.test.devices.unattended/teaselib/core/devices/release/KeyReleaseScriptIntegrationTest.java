package teaselib.core.devices.release;

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
    TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
    Devices devices = script.teaseLib.devices;
    DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);
    KeyRelease keyRelease = deviceCache.getDefaultDevice();
    Actuator actuator = keyRelease.actuators().available().get(1, TimeUnit.HOURS).get();

    @Before
    public void before() {
        script.debugger.resumeTime();
    }

    @Test
    public void testScriptEventsWithItem() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);
        Item restraints = cuffs.get();

        assertFalse(actuator.isRunning());
        long available = actuator.available(TimeUnit.SECONDS);
        assertEquals(0, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());
        script.reply("Keys placed, #title");

        script.say("Holding", Message.Delay10s);
        assertEquals(available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.completeAll();
        assertEquals(available - 10, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Are you ready?");
        script.reply("In a minute, #title");
        assertEquals("Hold duration not reset", available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();
        assertEquals(available - 10, actuator.remaining(TimeUnit.SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        cuffs.apply();
        assertEquals(0, script.events().afterChoices.size());
        assertEquals("Release timer not reset", available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.completeAll();
        assertEquals("Release timer wrong value", available - 10, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", available - 20, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(TimeUnit.SECONDS), 1.0);
    }

    @Test
    public void testScriptEventsWithItems() {
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        assertFalse(actuator.isRunning());
        long available = actuator.available(TimeUnit.SECONDS);
        assertEquals(0, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());
        script.reply("Keys placed, #title");

        script.say("Holding", Message.Delay10s);
        assertEquals(available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.completeAll();
        assertEquals(available - 10, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Are you ready?");
        script.reply("In a minute, #title");
        assertEquals("Hold duration not reset", available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();
        assertEquals(available - 10, actuator.remaining(TimeUnit.SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        restraints.apply();
        assertEquals(0, script.events().afterChoices.size());
        assertEquals("Release timer not reset", available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.completeAll();
        assertEquals("Release timer wrong value", available - 10, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", available - 20, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);

        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(TimeUnit.SECONDS), 1.0);
    }

    @Test
    @Ignore
    // TODO Device becomes disconnected while sending command during sleep, but reconnect fails
    public void testScriptEventsWithItemsAndSleepWhileHolding() {
        Items restraints = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        assertFalse(actuator.isRunning());
        long available = actuator.available(TimeUnit.SECONDS);
        assertEquals(0, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Arm", Message.Delay10s);
        script.script(KeyReleaseSetup.class).prepare(actuator, restraints);
        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());
        script.reply("Keys placed, #title");

        // Device will not react on commands
        actuator.sleep(15, TimeUnit.SECONDS);

        script.say("Holding", Message.Delay10s, "Are you ready?");
        script.reply("In a minute, #title");
        // Hold command fails since device is sleeping

        script.say("Are you ready?", Message.Delay10s);
        script.completeAll();
        // Woken up
        assertEquals(available - 20, actuator.remaining(TimeUnit.SECONDS), 1.0);
        assertTrue(actuator.isRunning());

        script.say("Starting release timer", Message.Delay10s);
        restraints.apply();
        assertEquals(0, script.events().afterChoices.size());
        assertEquals("Release timer not reset", available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        actuator.sleep(15, TimeUnit.SECONDS);
        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer wrong value", available - 20, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);

        Item wristCuffs = restraints.get(Toys.Wrist_Restraints);
        wristCuffs.remove();
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());

        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(TimeUnit.SECONDS), 1.0);
    }

    @After
    public void releaseAll() {
        keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(Actuator::release);
    }
}
