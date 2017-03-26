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

public class EnumStateMapsDurationTest extends EnumStateMaps {

    public EnumStateMapsDurationTest() {
        super(TestScript.getOne().teaseLib);
    }

    @Test
    public void testLockDurationIsAppliedToDirectPeers() {
        assertApplyChastityCage();

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage).over(24,
                TimeUnit.HOURS);

        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());
    }

    @Test
    public void testLockDurationOnInfiniteToy() {
        assertApplyChastityCage();

        state(Toys.Chastity_Cage)
                .apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(Duration.INFINITE, TimeUnit.SECONDS);
        assertFalse(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).expired());
        // false is correct, because we havn't set a duration for the lock yet.
        // As a result the lock "inherits" the duration of the cage

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage).over(24,
                TimeUnit.HOURS);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());

        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        assertRemoveKey();
    }

    @Test
    public void testElapedAlsoImplementsFreeSince() {
        state(Toys.Chastity_Cage).apply();
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Cage).remove();
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);
        assertEquals(24,
                state(Toys.Chastity_Cage).duration().elapsed(TimeUnit.HOURS));
    }

    private void assertApplyChastityCage() {
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).applied());

        state(Toys.Chastity_Cage).apply(Body.SomethingOnPenis,
                Body.CannotJerkOff);

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());
    }

    private void assertRemoveKey() {
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Cage).applied());

        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertFalse(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Device_Lock).remove();

        assertFalse(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Cage).remove();
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());

        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
    }
}
