package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Duration;
import teaselib.Toys;
import teaselib.test.TestScript;

public class StateMapsDurationTest extends StateMaps {
    enum Locks {
        Chastity_Device_Lock
    }

    public StateMapsDurationTest() {
        super(TestScript.getOne().teaseLib);
        teaseLib.freezeTime();
    }

    @Test
    public void testLockDurationIsAppliedToDirectPeers() {
        assertApplyChastityCage();

        state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());

        state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24, TimeUnit.HOURS);

        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());
    }

    @Test
    public void testLockDurationOnInfiniteToy() {
        assertApplyChastityCage();

        state(Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(Duration.INFINITE, TimeUnit.SECONDS);
        assertFalse(state(Toys.Chastity_Device).expired());

        state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).expired());
        // false is correct, because we haven't set a duration for the lock yet.
        // As a result the lock "inherits" the duration of the cage

        state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24, TimeUnit.HOURS);

        assertTrue(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());

        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        assertRemoveKey();
    }

    @Test
    public void testElapedAlsoImplementsFreeSince() {
        state(Toys.Chastity_Device).apply();
        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Toys.Chastity_Device).expired());

        state(Toys.Chastity_Device).remove();
        assertFalse(state(Toys.Chastity_Device).applied());
        assertTrue(state(Toys.Chastity_Device).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);
        assertEquals(24, state(Toys.Chastity_Device).duration().elapsed(TimeUnit.HOURS));
    }

    private void assertApplyChastityCage() {
        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).applied());

        state(Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff);

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());
    }

    private void assertRemoveKey() {
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device).applied());

        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertFalse(state(Toys.Chastity_Device).expired());

        state(Locks.Chastity_Device_Lock).remove();

        assertFalse(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Chastity_Device).expired());

        state(Toys.Chastity_Device).remove();
        assertFalse(state(Toys.Chastity_Device).applied());
        assertTrue(state(Toys.Chastity_Device).expired());

        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
    }
}
