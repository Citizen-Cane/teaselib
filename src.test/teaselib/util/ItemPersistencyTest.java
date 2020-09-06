package teaselib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.State.Persistence.Until;
import teaselib.TeaseScriptPersistence.Domain;
import teaselib.Toys;
import teaselib.test.TestScript;

public class ItemPersistencyTest {

    @Test
    public void testThatItemIsNotAppliedAfterAutoRemoval() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.item(Toys.Chastity_Device).apply().over(2, TimeUnit.HOURS).remember(Until.Removed);
        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertTrue(script.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertTrue(script.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertFalse(script.item(Toys.Chastity_Device).applied());
    }

    @Test
    public void testThatDomainItemsAreAutoRemoved() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Domain domain = script.domain("TestDomain");
        domain.item(Toys.Chastity_Device).apply().over(2, TimeUnit.HOURS).remember(Until.Removed);
        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertTrue(domain.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertTrue(domain.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertFalse(domain.item(Toys.Chastity_Device).applied());
    }

    @Test
    public void testThatDomainItemsAreAutoRemovedInMultipleDomains() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        Domain domain1 = script.domain("TestDomain1");
        domain1.item(Toys.Chastity_Device).apply().over(2, TimeUnit.HOURS).remember(Until.Removed);
        Domain domain2 = script.domain("TestDomain2");
        domain2.item(Toys.Chastity_Device).apply().over(4, TimeUnit.HOURS).remember(Until.Removed);

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertTrue(domain1.item(Toys.Chastity_Device).applied());
        assertTrue(domain2.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertTrue(domain1.item(Toys.Chastity_Device).applied());
        assertTrue(domain2.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertFalse(domain1.item(Toys.Chastity_Device).applied());
        assertTrue(domain2.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(3, TimeUnit.HOURS);
        script.triggerAutoRemove();
        assertFalse(domain1.item(Toys.Chastity_Device).applied());
        assertFalse(domain2.item(Toys.Chastity_Device).applied());
    }

    @Test
    public void testThatAutoRemoveIsOnlyAppliedAtStartup() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.item(Toys.Chastity_Device).apply().over(2, TimeUnit.HOURS);

        script.debugger.advanceTime(1, TimeUnit.HOURS);
        assertTrue(script.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(3, TimeUnit.HOURS);
        assertTrue(script.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(Long.MAX_VALUE - 3, TimeUnit.HOURS);
        assertTrue(script.item(Toys.Chastity_Device).applied());
    }

    @Test
    public void testThatAutoRemoveDurationIsCheckedAgainstSessionStartTime() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.item(Toys.Chastity_Device).apply().over(2, TimeUnit.HOURS).remember(Until.Removed);
        script.debugger.clearStateMaps();

        script.debugger.advanceTime(3, TimeUnit.HOURS);
        assertTrue("Auto Removal didn't account session startup time", script.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(Long.MAX_VALUE - 3, TimeUnit.HOURS);
        assertTrue("Auto Removal didn't account session startup time", script.item(Toys.Chastity_Device).applied());
    }

    @Test
    public void testThatRememberUntilIsPersisted() {
        TestScript script = TestScript.getOne();
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
