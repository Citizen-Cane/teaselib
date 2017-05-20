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

        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff);
        state(TEST_DOMAIN, Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24, TimeUnit.HOURS).remember();

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
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).expired());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
    }

    @Test
    public void testRememberedItemsAreCompletlyRemoved() {
        state(TEST_DOMAIN, Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(24, TimeUnit.HOURS).remember();

        assertEquals(6, persistence.storage.size());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertEquals(0, persistence.storage.size());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        state(TEST_DOMAIN, Toys.Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
    }

    @Test
    public void testRememberedItemsAreCompletlyRemovedAfterSessionRestore() {
        state(TEST_DOMAIN, Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(24, TimeUnit.HOURS).remember();

        // Simulate session end & restore
        clear();

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertEquals(6, persistence.storage.size());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertEquals(0, persistence.storage.size());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());

        State cannotJerkOff = state(TEST_DOMAIN, Body.CannotJerkOff);
        assertFalse(cannotJerkOff.applied());
    }
}
