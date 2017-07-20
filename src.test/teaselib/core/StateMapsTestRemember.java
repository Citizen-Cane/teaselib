package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.Toys;
import teaselib.hosts.DummyPersistence;
import teaselib.test.TestScript;

public class StateMapsTestRemember extends StateMaps {
    public static final String TEST_DOMAIN = "test";

    enum Locks {
        Chastity_Device_Lock
    }

    final DummyPersistence persistence;

    public StateMapsTestRemember() {
        this(TestScript.getOne());
    }

    StateMapsTestRemember(TestScript script) {
        super(script.teaseLib);
        persistence = script.persistence;
        teaseLib.freezeTime();
    }

    @Test
    public void testRememberIsShallow() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff);
        state(TEST_DOMAIN, Toys.Wrist_Restraints).applyTo(Body.WristsTiedBehindBack, Body.CantJerkOff);

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device).over(24, TimeUnit.HOURS).remember();

        assertUnrelatedStateIsNotAffected();
        assertRememberedToyAndPeersAreRemembered();
        assertRememberIsShallowAndIndirectlyReferencedPeersArentAffected();

    }

    private void assertRememberedToyAndPeersAreRemembered() {
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
    }

    private void assertUnrelatedStateIsNotAffected() {
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertTrue(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, Body.WristsTiedBehindBack).expired());
    }

    private void assertRememberIsShallowAndIndirectlyReferencedPeersArentAffected() {
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).expired());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).expired());
    }

    @Test
    public void testRememberedItemsAreCompletlyRemoved() {
        state(TEST_DOMAIN, Toys.Wrist_Restraints).applyTo(Body.WristsTiedBehindBack, Body.CantJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff)
                .over(24, TimeUnit.HOURS).remember();

        assertEquals(6, persistence.storage.size());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertEquals(0, persistence.storage.size());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());

        state(TEST_DOMAIN, Toys.Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
        assertFalse(state(TEST_DOMAIN, Body.CantJerkOff).applied());
    }

    @Test
    public void testRememberedItemsAreCompletlyRemovedAfterSessionRestore() {
        state(TEST_DOMAIN, Toys.Wrist_Restraints).applyTo(Body.WristsTiedBehindBack, Body.CantJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff)
                .over(24, TimeUnit.HOURS).remember();

        // Simulate session end & restore
        clear();

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());

        assertEquals(6, persistence.storage.size());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertEquals(0, persistence.storage.size());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());

        State cannotJerkOff = state(TEST_DOMAIN, Body.CantJerkOff);
        assertFalse(cannotJerkOff.applied());
    }
}
