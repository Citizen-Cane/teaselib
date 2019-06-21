package teaselib.core.devices.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

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
import teaselib.util.Items;

public class KeyReleaseScriptIntegrationTest {

    @Test
    public void testUsageInScript() {
        TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
        script.debugger.resumeTime();

        Devices devices = script.teaseLib.devices;
        DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);

        KeyRelease keyRelease = deviceCache.getDefaultDevice();
        Actuator actuator = keyRelease.actuators().get(1, TimeUnit.HOURS);

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

        // TODO remaining before arm should be == available
        long available = actuator.available(TimeUnit.SECONDS);

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

}
