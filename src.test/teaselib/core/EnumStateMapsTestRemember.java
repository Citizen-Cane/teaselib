package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Toys;
import teaselib.test.TestScript;

public class EnumStateMapsTestRemember extends EnumStateMaps {
    enum Locks {
        Chastity_Device_Lock
    }

    public EnumStateMapsTestRemember() {
        super(TestScript.getOne().teaseLib);
        teaseLib.freezeTime();
    }

    @Test
    public void testRememberIsShallow() {
        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Wrist_Restraints).applied());

        state(Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff);
        state(Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);

        state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24, TimeUnit.HOURS)
                .remember();

        assertUnrelatedStateIsNotAffected();
        assertRememberedToyAndPeersAreRemembered();
        assertRememberIsShallowAndIndirectlyReferencedPeersArentAffected();

    }

    private void assertRememberedToyAndPeersAreRemembered() {
        assertTrue(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).expired());
    }

    private void assertUnrelatedStateIsNotAffected() {
        assertTrue(state(Toys.Wrist_Restraints).applied());
        assertTrue(state(Body.WristsTiedBehindBack).applied());
        assertTrue(state(Toys.Wrist_Restraints).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
    }

    private void assertRememberIsShallowAndIndirectlyReferencedPeersArentAffected() {
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).expired());
        assertTrue(state(Body.SomethingOnPenis).expired());
    }
}
