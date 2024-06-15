package teaselib.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.*;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class StateMapsDurationTest extends TestableStateMaps {
    public static final String TEST_DOMAIN = "test";

    enum Locks {
        Chastity_Device_Lock
    }

    public StateMapsDurationTest() throws IOException {
        super(new TestScript());
        teaseLib.freezeTime();
    }

    @AfterEach
    public void cleanup() {
        teaseLib.close();
    }

    @Test
    public void testLockDurationIsAppliedToDirectPeers() {
        assertApplyChastityCage();

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device);

        Assertions.assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        Assertions.assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device).over(24, TimeUnit.HOURS);

        Assertions.assertTrue(state(TEST_DOMAIN, Body.OnPenis).expired());
        Assertions.assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).expired());
    }

    @Test
    public void testLockDurationOnInfiniteToy() {
        assertApplyChastityCage();

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff).over(Duration.INFINITE,
                TimeUnit.SECONDS);
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device);

        Assertions.assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        // false is correct, because we haven't set a duration for the lock yet.
        // As a result the lock "inherits" the duration of the cage

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device).over(24, TimeUnit.HOURS);

        Assertions.assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        Assertions.assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        Assertions.assertFalse(state(TEST_DOMAIN, Body.OnPenis).expired());
        Assertions.assertFalse(state(TEST_DOMAIN, Body.CantJerkOff).expired());

        assertRemoveKey();
    }

    @Test
    public void testElapedAlsoImplementsFreeSince() {
        State state = state(TEST_DOMAIN, Toys.Chastity_Device);
        state.apply();
        Assertions.assertTrue(state.applied());
        Assertions.assertTrue(state.expired());

        Duration sinceApplied = teaseLib.duration();
        teaseLib.advanceTime(1, TimeUnit.HOURS);
        Assertions.assertEquals(1, state.duration().elapsed(TimeUnit.HOURS));
        Assertions.assertEquals(1, sinceApplied.elapsed(TimeUnit.HOURS));

        state.remove();
        Assertions.assertFalse(state.applied());
        Assertions.assertTrue(state.expired());
        Assertions.assertEquals(1, sinceApplied.elapsed(TimeUnit.HOURS));
        Assertions.assertEquals(1, state.duration().elapsed(TimeUnit.HOURS));

        teaseLib.advanceTime(23, TimeUnit.HOURS);
        Assertions.assertEquals(1, state.duration().elapsed(TimeUnit.HOURS));
        Assertions.assertEquals(24, sinceApplied.elapsed(TimeUnit.HOURS));
    }

    @Test
    public void testDurationDependsOnPeers() {
        teaseLib.freezeTime();

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff).over(1, TimeUnit.HOURS);

        Assertions.assertEquals(1, state(TEST_DOMAIN, Toys.Chastity_Device).duration().remaining(TimeUnit.HOURS));
        Assertions.assertEquals(1, state(TEST_DOMAIN, Body.OnPenis).duration().remaining(TimeUnit.HOURS));
        Assertions.assertEquals(1, state(TEST_DOMAIN, Body.CantJerkOff).duration().remaining(TimeUnit.HOURS));

        state(TEST_DOMAIN, Bondage.Wrist_Restraints).applyTo(Posture.WristsTiedBehindBack, Body.CantJerkOff).over(2,
                TimeUnit.HOURS);

        Assertions.assertEquals(1, state(TEST_DOMAIN, Toys.Chastity_Device).duration().remaining(TimeUnit.HOURS));
        Assertions.assertEquals(1, state(TEST_DOMAIN, Body.OnPenis).duration().remaining(TimeUnit.HOURS));
        Assertions.assertEquals(2, state(TEST_DOMAIN, Body.CantJerkOff).duration().remaining(TimeUnit.HOURS));

        Assertions.assertEquals(2, state(TEST_DOMAIN, Bondage.Wrist_Restraints).duration().remaining(TimeUnit.HOURS));

        state(TEST_DOMAIN, Bondage.Wrist_Restraints).remove();

        Assertions.assertEquals(1, state(TEST_DOMAIN, Body.CantJerkOff).duration().remaining(TimeUnit.HOURS));
    }

    private void assertApplyChastityCage() {
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff);

        Assertions.assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());
        Assertions.assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());
        Assertions.assertTrue(state(TEST_DOMAIN, Body.OnPenis).expired());
        Assertions.assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).expired());
    }

    private void assertRemoveKey() {
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        Assertions.assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());

        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        Assertions.assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        Assertions.assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        Assertions.assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        Assertions.assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());
        Assertions.assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());
    }
}
