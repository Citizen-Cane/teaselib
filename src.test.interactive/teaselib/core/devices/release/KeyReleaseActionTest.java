package teaselib.core.devices.release;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.State;
import teaselib.Toys;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.Devices;
import teaselib.test.TestScript;

public class KeyReleaseActionTest {
    @Test
    public void testManualReleaseViaReleaseAction() {
        TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
        Devices devices = script.teaseLib.devices;
        DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);
        KeyRelease keyRelease = deviceCache.getDefaultDevice();

        for (Actuator actuator : KeyReleaseTest.connect(keyRelease)) {
            State releaseAction = actuator.releaseAction(script.teaseLib);
            KeyReleaseTest.arm(actuator);

            script.item(Toys.Wrist_Restraints).apply();
            script.item(Toys.Wrist_Restraints).applyTo(releaseAction);

            KeyReleaseTest.start(actuator);

            KeyReleaseTest.sleep(10, TimeUnit.SECONDS);

            assertTrue(actuator.isRunning());
            script.item(Toys.Wrist_Restraints).remove();
            assertFalse(actuator.isRunning());
        }

        KeyReleaseTest.assertEndState(keyRelease);
    }
}
