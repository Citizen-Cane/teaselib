package teaselib.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Toys;
import teaselib.test.TestScript;

public class ItemPersistencyTest {

    @Test
    public void testThatItemIsNotAppliedAfterAutoRemoval() {
        TestScript script = TestScript.getOne();
        script.debugger.freezeTime();

        script.item(Toys.Chastity_Device).apply().over(2, TimeUnit.HOURS);
        script.debugger.advanceTime(1, TimeUnit.HOURS);
        script.debugger.clearStateMaps();

        assertTrue(script.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(3, TimeUnit.HOURS);
        script.debugger.clearStateMaps();

        assertFalse(script.item(Toys.Chastity_Device).applied());
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

        script.item(Toys.Chastity_Device).apply().over(2, TimeUnit.HOURS);
        script.debugger.clearStateMaps();

        script.debugger.advanceTime(3, TimeUnit.HOURS);
        assertTrue("Auto Removal didn't account session startup time", script.item(Toys.Chastity_Device).applied());

        script.debugger.advanceTime(Long.MAX_VALUE - 3, TimeUnit.HOURS);
        assertTrue("Auto Removal didn't account session startup time", script.item(Toys.Chastity_Device).applied());
    }

}
