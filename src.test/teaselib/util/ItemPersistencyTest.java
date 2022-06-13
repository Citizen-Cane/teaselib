package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.TeaseScriptPersistence.Domain;
import teaselib.Toys;
import teaselib.test.TestScript;

public class ItemPersistencyTest {

    @Test
    public void testThatItemIsNotAppliedAfterAutoRemoval() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Chastity_Device);

            item.apply().over(2, TimeUnit.DAYS).remember(Until.Removed);
            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertFalse("Until.Removed should expire after 2 days + 0.5 * duration", item.applied());
        }
    }

    @Test
    public void testLastUsedItem() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Buttplug);

            item.apply().over(60, TimeUnit.MINUTES).remember(Until.Expired);
            script.debugger.advanceTime(60, TimeUnit.MINUTES);
            item.remove();
            script.debugger.advanceTime(60, TimeUnit.MINUTES);

            assertFalse(item.applied());
            assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
            assertEquals(60, item.removed(TimeUnit.MINUTES));
        }
    }

    @Test
    public void testLastUsedItemAutoRemove() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Buttplug);

            item.apply().over(60, TimeUnit.MINUTES).remember(Until.Expired);
            script.debugger.advanceTime(120, TimeUnit.MINUTES);
            script.triggerAutoRemove();

            assertFalse(item.applied());
            assertEquals(60, item.removed(TimeUnit.MINUTES));
            assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
        }
    }

    @Test
    public void testLastUsedState() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            State state = script.state(Toys.Buttplug);

            state.applyTo(Body.InButt).over(60, TimeUnit.MINUTES).remember(Until.Expired);
            script.debugger.advanceTime(60, TimeUnit.MINUTES);
            state.remove();
            script.debugger.advanceTime(60, TimeUnit.MINUTES);

            assertFalse(state.applied());
            assertEquals(60, state.removed(TimeUnit.MINUTES));
            assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
        }
    }

    @Test
    public void testLastUsedStateAutoRemove() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            State state = script.state(Toys.Buttplug);

            state.applyTo(Body.InButt).over(60, TimeUnit.MINUTES).remember(Until.Expired);
            script.debugger.advanceTime(120, TimeUnit.MINUTES);
            script.triggerAutoRemove();

            assertFalse(state.applied());
            assertEquals(60, state.removed(TimeUnit.MINUTES));
            assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
        }
    }

    @Test
    public void testThatDomainItemsAreAutoRemoved() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Domain domain = script.domain("TestDomain");
            Item item = domain.item(Toys.Chastity_Device);

            item.apply().over(2, TimeUnit.DAYS).remember(Until.Removed);
            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertFalse("Until.Removed should expire after 2 days + 0.5 * duration", item.applied());
        }
    }

    @Test
    public void testThatDomainItemsAreAutoRemovedInMultipleDomains() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();

            Domain domain1 = script.domain("TestDomain1");
            domain1.item(Toys.Chastity_Device).apply().over(2, TimeUnit.DAYS).remember(Until.Removed);
            Domain domain2 = script.domain("TestDomain2");
            domain2.item(Toys.Chastity_Device).apply().over(4, TimeUnit.DAYS).remember(Until.Removed);

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertTrue(domain1.item(Toys.Chastity_Device).applied());
            assertTrue(domain2.item(Toys.Chastity_Device).applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertTrue("Until.Removed should expire after 2 days + 0.5 * duration",
                    domain1.item(Toys.Chastity_Device).applied());
            assertTrue(domain2.item(Toys.Chastity_Device).applied());

            script.debugger.advanceTime(3, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertFalse(domain1.item(Toys.Chastity_Device).applied());
            assertTrue(domain2.item(Toys.Chastity_Device).applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            assertFalse(domain1.item(Toys.Chastity_Device).applied());
            assertFalse(domain2.item(Toys.Chastity_Device).applied());
        }
    }

    @Test
    public void testThatAutoRemoveIsOnlyAppliedAtStartup() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Chastity_Device);

            item.apply().over(2, TimeUnit.HOURS);

            script.debugger.advanceTime(1, TimeUnit.HOURS);
            assertTrue(item.applied());

            script.debugger.advanceTime(3, TimeUnit.HOURS);
            assertTrue(item.applied());

            script.debugger.advanceTime(Long.MAX_VALUE - 3, TimeUnit.HOURS);
            assertTrue(item.applied());
        }
    }

    @Test
    public void testThatAutoRemoveDurationIsCheckedAgainstSessionStartTime() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Chastity_Device);

            item.apply().over(2, TimeUnit.HOURS).remember(Until.Removed);
            script.debugger.clearStateMaps();

            script.debugger.advanceTime(3, TimeUnit.HOURS);
            assertTrue("Auto Removal didn't account session startup time", item.applied());

            script.debugger.advanceTime(Long.MAX_VALUE - 3, TimeUnit.HOURS);
            assertTrue("Auto Removal didn't account session startup time", item.applied());
        }
    }

    @Test
    public void testThatRememberUntilIsPersisted() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Chastity_Device);

            item.apply().remember(Until.Removed);
            item.is(Until.Removed);
            String dispayName = item.displayName();

            script.debugger.clearStateMaps();

            Item restored = script.item(Toys.Chastity_Device);
            assertEquals(dispayName, restored.displayName());
            assertTrue(restored.is(Until.Removed));
        }
    }

    @Test
    public void testThatLastUsedIsPersisted() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Buttplug);

            item.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
            assertEquals(0, item.duration().elapsed(TimeUnit.HOURS));

            script.debugger.advanceTime(1, TimeUnit.HOURS);
            assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));

            script.debugger.clearStateMaps();
            assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));

            item.remove();
            assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));

            script.debugger.clearStateMaps();
            assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));
        }
    }

}
