package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import teaselib.Body;
import teaselib.Posture;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.test.TestScript;

public class StateMapsTestRemember extends TestableStateMaps {
    public static final String TEST_DOMAIN = "test";

    private enum Locks {
        Chastity_Device_Lock
    }

    private final TestScript script;

    public StateMapsTestRemember() throws IOException {
        this(new TestScript());
    }

    private StateMapsTestRemember(TestScript script) {
        super(script.teaseLib);
        this.script = script;
        teaseLib.freezeTime();
    }

    @After
    public void cleanup() {
        script.close();
    }

    @Test
    public void testRememberIsShallow() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff);
        state(TEST_DOMAIN, Toys.Wrist_Restraints).applyTo(Posture.WristsTiedBehindBack, Body.CantJerkOff);

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Toys.Chastity_Device).over(24, TimeUnit.HOURS)
                .remember(Until.Removed);

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
        assertTrue(state(TEST_DOMAIN, Posture.WristsTiedBehindBack).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, Posture.WristsTiedBehindBack).expired());
    }

    private void assertRememberIsShallowAndIndirectlyReferencedPeersArentAffected() {
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).expired());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).expired());
    }

    @Test
    public void testRememberedItemsAreCompletelyRemoved() {
        state(TEST_DOMAIN, Toys.Wrist_Restraints).applyTo(Posture.WristsTiedBehindBack, Body.CantJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff).over(24, TimeUnit.HOURS)
                .remember(Until.Removed);

        assertEquals(15, script.storageSize());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertEquals(3, script.storageSize());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());

        state(TEST_DOMAIN, Toys.Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Posture.WristsTiedBehindBack).applied());
        assertFalse(state(TEST_DOMAIN, Body.CantJerkOff).applied());
    }

    @Test
    public void testRememberedItemsAreCompletelyRemovedAfterSessionRestore() {
        state(TEST_DOMAIN, Toys.Wrist_Restraints).applyTo(Posture.WristsTiedBehindBack, Body.CantJerkOff);
        state(TEST_DOMAIN, Toys.Chastity_Device).applyTo(Body.OnPenis, Body.CantJerkOff).over(24, TimeUnit.HOURS)
                .remember(Until.Removed);

        // Simulate session end & restore
        clear();

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Posture.WristsTiedBehindBack).applied());

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.OnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CantJerkOff).applied());

        assertEquals(15, script.storageSize());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertEquals(3, script.storageSize());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.OnPenis).applied());

        State cannotJerkOff = state(TEST_DOMAIN, Body.CantJerkOff);
        assertFalse(cannotJerkOff.applied());
    }
}
