package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import teaselib.Body;
import teaselib.State;
import teaselib.Toys;
import teaselib.hosts.DummyPersistence;
import teaselib.test.TestScript;

@RunWith(Parameterized.class)
public class StateMapsPersistenceTest extends StateMaps {
    public static final String TEST_DOMAIN = "test";

    enum Locks {
        Chastity_Device_Lock
    }

    final DummyPersistence persistence;

    static TestScript script;

    TestParameter rememberState;

    @BeforeClass
    public static void initPersistence() {
        script = TestScript.getOne();
        script.teaseLib.freezeTime();
    }

    enum TestParameter {
        DontTestPersistence,
        TestPersistence
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] { { TestParameter.DontTestPersistence },
                { TestParameter.TestPersistence } });
    }

    public StateMapsPersistenceTest(TestParameter remember) {
        super(script.teaseLib);
        persistence = script.persistence;
        rememberState = remember;
    }

    @Before
    public void initStorage() {
        persistence.storage.clear();

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
    }

    void rememberOrNot(State.Persistence state) {
        if (isRemembered()) {
            state.remember();
        }
    }

    void clearStatesMapsOrNot() {
        if (isRemembered()) {
            persistence.printStorage();
            clear();
        }
    }

    private boolean isRemembered() {
        return rememberState == TestParameter.TestPersistence;
    }

    private boolean isTemporary() {
        return !isRemembered();
    }

    @Test
    public void testPersistenceOnLock() {
        rememberOrNot(state(TEST_DOMAIN, Toys.Chastity_Device).apply(Body.SomethingOnPenis,
                Body.CannotJerkOff));

        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device)
                .over(24, TimeUnit.HOURS));

        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).expired());

        clearStatesMapsOrNot();

        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).expired());

        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());

        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
    }

    @Test
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBack() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());

        state(TEST_DOMAIN, Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack,
                Body.CannotJerkOff);
        rememberOrNot(state(TEST_DOMAIN, Toys.Chastity_Device)
                .apply(Body.SomethingOnPenis, Body.CannotJerkOff).over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertEquals(isTemporary(), state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertEquals(isTemporary(), state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertEquals(isTemporary(), state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertEquals(isTemporary(), state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());

        state(TEST_DOMAIN, Toys.Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
    }

    @Test
    public void testCannotJerkOffWhenWearingALockedChastityCageAndHandsTiedOnBack() {
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());

        rememberOrNot(state(TEST_DOMAIN, Toys.Chastity_Device)
                .apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(State.INDEFINITELY, TimeUnit.HOURS));

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device)
                .over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        teaseLib.advanceTime(22, TimeUnit.HOURS);

        state(TEST_DOMAIN, Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack,
                Body.CannotJerkOff);

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).expired());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        state(TEST_DOMAIN, Toys.Wrist_Restraints).remove();

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, Body.WristsTiedBehindBack).expired());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        // cage is applied indefinitely
        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, Body.WristsTiedBehindBack).expired());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, Body.WristsTiedBehindBack).expired());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).expired());

        state(TEST_DOMAIN, Toys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, Toys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, Toys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body.WristsTiedBehindBack).applied());
        assertFalse(state(TEST_DOMAIN, Body.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, Body.CannotJerkOff).applied());

        assertTrue(state(TEST_DOMAIN, Toys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, Toys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, Body.WristsTiedBehindBack).expired());
        assertTrue(state(TEST_DOMAIN, Body.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, Body.CannotJerkOff).expired());
    }

    @Test
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBackWithStrings() {
        String Toys_Chastity_Device = "Toys.Chastity_Device";
        String Toys_Wrist_Restraints = "Toys.Wrist_Restraints";

        String Body_WristsTiedBehindBack = "Body.WristsTiedBehindBack";
        String Body_SomethingOnPenis = "Body.SomethingOnPenis";
        String Body_CannotJerkOff = "Body.CannotJerkOff";

        assertFalse(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());

        state(TEST_DOMAIN, Toys_Wrist_Restraints).apply(Body_WristsTiedBehindBack,
                Body_CannotJerkOff);
        rememberOrNot(state(TEST_DOMAIN, Toys_Chastity_Device)
                .apply(Body_SomethingOnPenis, Body_CannotJerkOff).over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        assertTrue(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body_SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body_CannotJerkOff).applied());

        assertEquals(isTemporary(), state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertEquals(isTemporary(), state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());

        state(TEST_DOMAIN, Toys_Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body_SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body_CannotJerkOff).applied());

        assertEquals(isTemporary(), state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertEquals(isTemporary(), state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());

        if (isRemembered()) {
            assertTrue(
                    script.persistence.storage.containsKey(Body_CannotJerkOff + ".state.duration"));
            assertTrue(script.persistence.storage.containsKey(Body_CannotJerkOff + ".state.peers"));
        }

        state(TEST_DOMAIN, Toys_Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());
    }

}
