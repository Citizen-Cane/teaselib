package teaselib.core.devices.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import teaselib.Message;
import teaselib.Toys;
import teaselib.core.ScriptEventArgs;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.core.events.Event;
import teaselib.core.events.EventSource;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class KeyReleaseScriptIntegrationTest {
    TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
    Devices devices = script.teaseLib.devices;
    DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);
    KeyRelease keyRelease = deviceCache.getDefaultDevice();
    Actuator actuator = keyRelease.actuators().get(1, TimeUnit.HOURS);

    @Before
    public void before() {
        script.debugger.resumeTime();
    }

    @Test
    @Ignore
    public void testUsageInScriptWithActionStates() {
        Items cuffs = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints);

        // TODO make applyAction being called on cuffs.apply() instead of here
        // -> works with releaseAction on remove since it's called on the first remove()
        script.state(actuator.applyAction()).applyTo(cuffs);
        script.state(actuator.releaseAction()).applyTo(cuffs);

        EventSource<ScriptEventArgs> afterChoices = script.events().afterChoices;
        Event<ScriptEventArgs> renewHold = new Event<ScriptEventArgs>() {
            @Override
            public void run(ScriptEventArgs eventArgs) throws Exception {
                if (actuator.isRunning()) {
                    actuator.hold();
                } else {
                    afterChoices.remove(this);
                }
            }
        };
        afterChoices.add(renewHold);

        long available = actuator.available(TimeUnit.SECONDS);
        assertEquals(available, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Arm", Message.Delay10s);
        actuator.arm();
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
        // TODO Start not called
        cuffs.apply();
        script.completeAll();
        assertEquals("Release timer not set", available - 10, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Timer is running", Message.Delay10s);
        script.completeAll();
        assertEquals("Release timer not set", available - 20, actuator.remaining(TimeUnit.SECONDS), 1.0);

        script.say("Releasing key", Message.Delay10s);
        cuffs.remove();
        assertFalse(actuator.isRunning());
        assertEquals(0, actuator.remaining(TimeUnit.SECONDS), 1.0);
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
        assertEquals(1, script.events().itemApplied.size());
        assertEquals(1, script.events().itemRemoved.size());
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
        assertEquals(1, script.events().itemApplied.size());
        assertEquals(1, script.events().itemRemoved.size());
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

    // TODO Remove hold hook and test it

    @After
    public void releaseAll() {
        keyRelease.actuators().stream().filter(Actuator::isRunning).forEach(Actuator::release);
    }
}
