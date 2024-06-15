package teaselib.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.Body;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.TeaseScriptPersistence.Domain;
import teaselib.Toys;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ItemPersistencyTest {

    @Test
    public void testThatItemIsNotAppliedAfterAutoRemoval() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Chastity_Device);

            item.apply().over(2, TimeUnit.DAYS).remember(Until.Removed);
            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertFalse(item.applied(), "Until.Removed should expire after 2 days + 0.5 * duration");
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

            Assertions.assertFalse(item.applied());
            Assertions.assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, item.removed(TimeUnit.MINUTES));
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

            Assertions.assertFalse(item.applied());
            Assertions.assertEquals(60, item.removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
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

            Assertions.assertFalse(state.applied());
            Assertions.assertEquals(60, state.removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
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

            Assertions.assertFalse(state.applied());
            Assertions.assertEquals(60, state.removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, script.state(Toys.Buttplug).removed(TimeUnit.MINUTES));
            Assertions.assertEquals(60, script.state(Body.InButt).removed(TimeUnit.MINUTES));
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
            Assertions.assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertTrue(item.applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertFalse(item.applied(), "Until.Removed should expire after 2 days + 0.5 * duration");
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
            Assertions.assertTrue(domain1.item(Toys.Chastity_Device).applied());
            Assertions.assertTrue(domain2.item(Toys.Chastity_Device).applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertTrue(domain1.item(Toys.Chastity_Device).applied(), "Until.Removed should expire after 2 days + 0.5 * duration");
            Assertions.assertTrue(domain2.item(Toys.Chastity_Device).applied());

            script.debugger.advanceTime(3, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertFalse(domain1.item(Toys.Chastity_Device).applied());
            Assertions.assertTrue(domain2.item(Toys.Chastity_Device).applied());

            script.debugger.advanceTime(1, TimeUnit.DAYS);
            script.triggerAutoRemove();
            Assertions.assertFalse(domain1.item(Toys.Chastity_Device).applied());
            Assertions.assertFalse(domain2.item(Toys.Chastity_Device).applied());
        }
    }

    @Test
    public void testThatAutoRemoveIsOnlyAppliedAtStartup() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Chastity_Device);

            item.apply().over(2, TimeUnit.HOURS);

            script.debugger.advanceTime(1, TimeUnit.HOURS);
            Assertions.assertTrue(item.applied());

            script.debugger.advanceTime(3, TimeUnit.HOURS);
            Assertions.assertTrue(item.applied());

            script.debugger.advanceTime(Long.MAX_VALUE - 3, TimeUnit.HOURS);
            Assertions.assertTrue(item.applied());
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
            Assertions.assertTrue(item.applied(), "Auto Removal didn't account session startup time");

            script.debugger.advanceTime(Long.MAX_VALUE - 3, TimeUnit.HOURS);
            Assertions.assertTrue(item.applied(), "Auto Removal didn't account session startup time");
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
            Assertions.assertEquals(dispayName, restored.displayName());
            Assertions.assertTrue(restored.is(Until.Removed));
        }
    }

    @Test
    public void testThatLastUsedIsPersisted() throws IOException {
        try (TestScript script = new TestScript()) {
            script.debugger.freezeTime();
            Item item = script.item(Toys.Buttplug);

            item.apply().over(1, TimeUnit.HOURS).remember(Until.Removed);
            Assertions.assertEquals(0, item.duration().elapsed(TimeUnit.HOURS));

            script.debugger.advanceTime(1, TimeUnit.HOURS);
            Assertions.assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));

            script.debugger.clearStateMaps();
            Assertions.assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));

            item.remove();
            Assertions.assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));

            script.debugger.clearStateMaps();
            Assertions.assertEquals(1, item.duration().elapsed(TimeUnit.HOURS));
        }
    }

}
