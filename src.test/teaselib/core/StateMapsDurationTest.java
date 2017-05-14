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
    public static final String TEST_DOMAIN = "test";

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

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24,
                TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).expired());
    }

    @Test
    public void testLockDurationOnInfiniteToy() {
        assertApplyChastityCage();

        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(Duration.INFINITE, TimeUnit.SECONDS);
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        // false is correct, because we haven't set a duration for the lock yet.
        // As a result the lock "inherits" the duration of the cage

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24,
                TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).expired());

        assertRemoveKey();
    }

    @Test
    public void testElapedAlsoImplementsFreeSince() {
        state(TEST_DOMAIN, Toys.Chastity_Device).apply();
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);
        assertEquals(24,
                state(TEST_DOMAIN, Toys.Chastity_Device).duration().elapsed(TimeUnit.HOURS));
    }

    private void assertApplyChastityCage() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff);

        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).expired());
    }

    private void assertRemoveKey() {
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());

        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
    }
}