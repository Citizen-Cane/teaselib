package teaselib.core.devices.release;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void testHoldAndAttachAction() {
        TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
        Devices devices = script.teaseLib.devices;
        DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);
        KeyRelease keyRelease = deviceCache.getDefaultDevice();

        for (Actuator actuator : KeyReleaseTest.connect(keyRelease)) {
            // TODO Resolve white space persistency issue since
            // the test fails badly when the device name contains white space
            State releaseAction = script.state(actuator.releaseAction());
            releaseAction.applyTo(Toys.Wrist_Restraints);
            // TODO assertFalse(item(Toys.Wrists_Restraints).applyTo(releaseAction));
            assertFalse(script.item(Toys.Wrist_Restraints).applied());

            State applyAction = script.state(actuator.applyAction());
            // TODO In KeyReleaseActionsTest.testThatActionStatesCanBeAttachedBeforehandWithoutApplyingToPeer
            // the action state is applied to item instances - in Items collection
            // - here we apply to item values only
            applyAction.applyTo(Toys.Wrist_Restraints);

            // TODO assertFalse(item(Toys.Wrists_Restraints).applyTo(applyAction));
            assertFalse(script.item(Toys.Wrist_Restraints).applied());

            KeyReleaseTest.arm(actuator);
            KeyReleaseTest.sleep(5, TimeUnit.SECONDS);
            // TODO
            // assertFalse(actuator.isRunning());

            KeyReleaseTest.hold(actuator);
            KeyReleaseTest.sleep(5, TimeUnit.SECONDS);
            // TODO
            // assertFalse(actuator.isRunning());

            script.item(Toys.Wrist_Restraints).apply();
            assertTrue(actuator.isRunning());

            KeyReleaseTest.sleep(5, TimeUnit.SECONDS);

            assertTrue(actuator.isRunning());
            script.item(Toys.Wrist_Restraints).remove();
            assertFalse(actuator.isRunning());
        }

        KeyReleaseTest.assertEndState(keyRelease);
    }

    @Test
    public void testManualReleaseViaReleaseAction() {
        TestScript script = TestScript.getOne(new DebugSetup().withRemoteDeviceAccess());
        Devices devices = script.teaseLib.devices;
        DeviceCache<KeyRelease> deviceCache = devices.get(KeyRelease.class);
        KeyRelease keyRelease = deviceCache.getDefaultDevice();

        for (Actuator actuator : KeyReleaseTest.connect(keyRelease)) {
            // TODO Resolve white space persistency issue since
            // the test fails badly when the device name contains white space
            State releaseAction = script.state(actuator.releaseAction());
            KeyReleaseTest.arm(actuator);

            script.item(Toys.Wrist_Restraints).apply();
            script.item(Toys.Wrist_Restraints).applyTo(releaseAction);

            KeyReleaseTest.start(actuator);

            KeyReleaseTest.sleep(5, TimeUnit.SECONDS);

            assertTrue(actuator.isRunning());
            script.item(Toys.Wrist_Restraints).remove();
            assertFalse(actuator.isRunning());
        }

        KeyReleaseTest.assertEndState(keyRelease);
    }
}
