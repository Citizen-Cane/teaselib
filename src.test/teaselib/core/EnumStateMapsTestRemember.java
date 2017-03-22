package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.Toys;
import teaselib.test.TestScript;

public class EnumStateMapsTestRemember extends EnumStateMaps {

    public EnumStateMapsTestRemember() {
        super(TestScript.getOne().teaseLib);
    }

    @Test
    public void testRememberIsShallow() {
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());

        state(Toys.Chastity_Cage).apply(Body.SomethingOnPenis,
                Body.CannotJerkOff);
        state(Toys.Leather_Wrist_Cuffs).apply(Body.WristsTiedBehindBack,
                Body.CannotJerkOff);

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage)
                .upTo(24, TimeUnit.HOURS).remember();

        assertUnrelatedStateIsNotAffected();
        assertRememberedToyAndPeersAreRemembered();
        assertRememberIsShallowAndIndirectlyReferencedPeersArentAffected();

    }

    private void assertRememberedToyAndPeersAreRemembered() {
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).expired());
    }

    private void assertUnrelatedStateIsNotAffected() {
        assertTrue(state(Toys.Leather_Wrist_Cuffs).applied());
        assertTrue(state(Body.WristsTiedBehindBack).applied());
        assertTrue(state(Toys.Leather_Wrist_Cuffs).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
    }

    private void assertRememberIsShallowAndIndirectlyReferencedPeersArentAffected() {
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).expired());
        assertTrue(state(Body.SomethingOnPenis).expired());
    }
}
