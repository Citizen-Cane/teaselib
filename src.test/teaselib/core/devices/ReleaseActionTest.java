package teaselib.core.devices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import teaselib.State;
import teaselib.Toys;
import teaselib.core.StateImpl;
import teaselib.core.TeaseLib;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyRelease;
import teaselib.core.util.Persist;
import teaselib.test.TestScript;
import teaselib.util.Item;

public class ReleaseActionTest {
    public static final class ReleaseActionState extends StateImpl implements Persist.Persistable {
        static final AtomicBoolean Success = new AtomicBoolean(false);

        public ReleaseActionState(TeaseLib teaseLib, String devicePath) {
            super(teaseLib, TeaseLib.DefaultDomain, devicePath);
        }

        @Override
        public List<String> persisted() {
            return Arrays.asList(Persist.persist(domain), Persist.persist(item.toString()));
        }

        public ReleaseActionState(Persist.Storage storage) {
            super(storage.getInstance(TeaseLib.class), storage.next(), storage.next());
        }

        public String devicePath() {
            return item.toString();
        }

        @Override
        public Persistence remove() {
            Success.set(true);
            return this;
        }
    }

    @Test
    public void testReleasectionStateQualified() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = new ReleaseActionState(script.teaseLib, devicePath);
        String action = Persist.persist(actionItem);
        ReleaseActionState restored = (ReleaseActionState) Persist.from(action, clazz -> script.teaseLib);
        assertEquals(devicePath, restored.devicePath());

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(action);

        // start(action);

        restraints.remove();

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));
    }

    @Test
    public void testReleaseActionInstance() {
        TestScript script = TestScript.getOne();

        String devicePath = "KeyRelease/MyPhoton/1";
        ReleaseActionState actionItem = new ReleaseActionState(script.teaseLib, devicePath);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(actionItem);

        // start(action);

        restraints.remove();

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));
    }

    @Test
    @Ignore
    // TODO simulate device
    public void testActuatorReleaseAction() {
        TestScript script = TestScript.getOne();

        KeyRelease device = script.teaseLib.devices.get(KeyRelease.class).getDefaultDevice();

        assertTrue(device.active());
        assertTrue(device.actuators().size() > 0);

        Actuator actuator = device.actuators().get(0);
        State release = actuator.releaseAction(script.teaseLib);

        Item restraints = script.item(Toys.Wrist_Restraints);
        restraints.apply();
        restraints.applyTo(release);

        // start(actuator);

        assertTrue(device.active());
        assertTrue(actuator.isRunning());
        restraints.remove();
        assertFalse(actuator.isRunning());

        assertEquals(true, ReleaseActionState.Success.getAndSet(false));
    }
}
