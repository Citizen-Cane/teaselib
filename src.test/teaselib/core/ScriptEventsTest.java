package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import teaselib.Toys;
import teaselib.core.devices.BatteryLevel;
import teaselib.core.devices.release.Actuator;
import teaselib.core.devices.release.KeyReleaseSetup;
import teaselib.test.TestScript;
import teaselib.util.Item;
import teaselib.util.Items;

public class ScriptEventsTest {
    class ActuatorMock implements Actuator {
        final AtomicBoolean active = new AtomicBoolean(false);

        @Override
        public String getDevicePath() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public boolean connected() {
            return false;
        }

        @Override
        public boolean active() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public boolean isWireless() {
            return false;
        }

        @Override
        public BatteryLevel batteryLevel() {
            return null;
        }

        @Override
        public int index() {
            return 0;
        }

        @Override
        public boolean arm() {
            return false;
        }

        @Override
        public void hold() {
        }

        @Override
        public void start() {
            active.set(true);
        }

        @Override
        public void start(long duration, TimeUnit unit) {
        }

        @Override
        public int sleep(long duration, TimeUnit unit) {
            return 0;
        }

        @Override
        public boolean add(long duration, TimeUnit unit) {
            return false;
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public long available(TimeUnit unit) {
            return 0;
        }

        @Override
        public long remaining(TimeUnit unit) {
            return 0;
        }

        @Override
        public boolean release() {
            return active.getAndSet(false);
        }
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveAllItems() {
        TestScript script = TestScript.getOne();
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        ActuatorMock actuator = new ActuatorMock();
        script.script(KeyReleaseSetup.class).prepare(actuator, items);

        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        items.apply();
        assertTrue(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        items.remove();
        assertFalse(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveOneItem() {
        TestScript script = TestScript.getOne();
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);

        ActuatorMock actuator = new ActuatorMock();
        script.script(KeyReleaseSetup.class).prepare(actuator, items);

        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        items.apply();
        assertTrue(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        items.get(Toys.Wrist_Restraints).remove();
        assertFalse(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveFromPeers() {
        TestScript script = TestScript.getOne();
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        ActuatorMock actuator = new ActuatorMock();
        script.script(KeyReleaseSetup.class).prepare(actuator, chains);

        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        chains.applyTo(items);
        assertTrue(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        chains.removeFrom(items);
        assertFalse(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());
    }

    @Test
    public void testKeyReleaseEventHandlingRemoveFromOnePeer() {
        TestScript script = TestScript.getOne();
        Items items = script.items(Toys.Wrist_Restraints, Toys.Ankle_Restraints, Toys.Collar);
        Item chains = script.item(Toys.Chains);

        ActuatorMock actuator = new ActuatorMock();
        script.script(KeyReleaseSetup.class).prepare(actuator, chains);

        assertEquals(1, script.events().afterChoices.size());
        assertEquals(2, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        chains.applyTo(items);
        assertTrue(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(2, script.events().itemRemoved.size());

        chains.removeFrom(items.get(Toys.Collar));
        assertFalse(actuator.active.get());
        assertEquals(0, script.events().afterChoices.size());
        assertEquals(0, script.events().itemApplied.size());
        assertEquals(0, script.events().itemRemoved.size());
    }

}
