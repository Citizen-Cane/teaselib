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

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device).over(24, TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Body.OnPenis).expired());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).expired());
    }

    @Test
    public void testLockDurationOnInfiniteToy() {
        assertApplyChastityCage();

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff)
                .over(Duration.INFINITE, TimeUnit.SECONDS);
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        // false is correct, because we haven't set a duration for the lock yet.
        // As a result the lock "inherits" the duration of the cage

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device).over(24, TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        assertFalse(state(TEST_DOMAIN, Body.OnPenis).expired());
        assertFalse(state(TEST_DOMAIN, Body.CantJerkOff).expired());

        assertRemoveKey();
    }

    @Test
    public void testElapedAlsoImplementsFreeSince() {
        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo();
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);
        assertEquals(24, state(TEST_DOMAIN, Toys.Chastity_Device).duration().elapsed(TimeUnit.HOURS));
    }

    @Test
    public void testDurationDependsOnPeers() {
        teaseLib.freezeTime();

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff).over(1,
                TimeUnit.HOURS);

        assertEquals(1, state(TEST_DOMAIN, Toys.Chastity_Device).duration().remaining(TimeUnit.HOURS));
        assertEquals(1, state(TEST_DOMAIN, Body.OnPenis).duration().remaining(TimeUnit.HOURS));
        assertEquals(1, state(TEST_DOMAIN, Body.CantJerkOff).duration().remaining(TimeUnit.HOURS));

        state(TEST_DOMAIN, Toys.Wrist_Restraints).applyTo(Body.WristsTiedBehindBack, Body.CantJerkOff).over(2,
                TimeUnit.HOURS);

        assertEquals(1, state(TEST_DOMAIN, Toys.Chastity_Device).duration().remaining(TimeUnit.HOURS));
        assertEquals(1, state(TEST_DOMAIN, Body.OnPenis).duration().remaining(TimeUnit.HOURS));
        assertEquals(2, state(TEST_DOMAIN, Body.CantJerkOff).duration().remaining(TimeUnit.HOURS));

        assertEquals(2, state(TEST_DOMAIN, Toys.Wrist_Restraints).duration().remaining(TimeUnit.HOURS));

        state(TEST_DOMAIN, Toys.Wrist_Restraints).remove();

        assertEquals(1, state(TEST_DOMAIN, Body.CantJerkOff).duration().remaining(TimeUnit.HOURS));
    }

    private void assertApplyChastityCage() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff);

        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).expired());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).expired());
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

        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());
    }
}
