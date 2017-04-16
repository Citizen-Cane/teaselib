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
public class EnumStateMapsPersistenceTest extends EnumStateMaps {
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

    public EnumStateMapsPersistenceTest(TestParameter remember) {
        super(script.teaseLib);
        persistence = script.persistence;
        rememberState = remember;
    }

    @Before
    public void initStorage() {
        persistence.storage.clear();

        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Wrist_Restraints).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
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
        rememberOrNot(state(Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff));

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device);

        assertTrue(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());

        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());

        rememberOrNot(state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24,
                TimeUnit.HOURS));

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

        clearStatesMapsOrNot();

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).expired());
        assertFalse(state(Toys.Chastity_Device).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Chastity_Device).expired());

        state(Locks.Chastity_Device_Lock).remove();

        assertFalse(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Toys.Chastity_Device).expired());

        state(Toys.Chastity_Device).remove();

        assertFalse(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertFalse(state(Toys.Chastity_Device).applied());
        assertTrue(state(Toys.Chastity_Device).expired());

        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
    }

    @Test
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBack() {
        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Wrist_Restraints).applied());

        state(Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);
        rememberOrNot(state(Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertEquals(isTemporary(), state(Toys.Wrist_Restraints).applied());
        assertEquals(isTemporary(), state(Body.WristsTiedBehindBack).applied());

        state(Toys.Chastity_Device).remove();

        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertEquals(isTemporary(), state(Toys.Wrist_Restraints).applied());
        assertEquals(isTemporary(), state(Body.WristsTiedBehindBack).applied());

        state(Toys.Wrist_Restraints).remove();

        assertFalse(state(Toys.Wrist_Restraints).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
    }

    @Test
    public void testCannotJerkOffWhenWearingALockedChastityCageAndHandsTiedOnBack() {
        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Toys.Wrist_Restraints).applied());

        rememberOrNot(state(Toys.Chastity_Device).apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(State.INDEFINITELY, TimeUnit.HOURS));

        rememberOrNot(state(Locks.Chastity_Device_Lock).apply(Toys.Chastity_Device).over(24,
                TimeUnit.HOURS));

        clearStatesMapsOrNot();

        teaseLib.advanceTime(22, TimeUnit.HOURS);

        state(Toys.Wrist_Restraints).apply(Body.WristsTiedBehindBack, Body.CannotJerkOff);

        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Wrist_Restraints).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Chastity_Device).expired());
        assertFalse(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Wrist_Restraints).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        state(Toys.Wrist_Restraints).remove();

        assertTrue(state(Toys.Chastity_Device).applied());
        assertTrue(state(Locks.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Wrist_Restraints).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Chastity_Device).expired());
        assertFalse(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Wrist_Restraints).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        // cage is applied indefinitely
        assertFalse(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Wrist_Restraints).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        state(Locks.Chastity_Device_Lock).remove();

        assertTrue(state(Toys.Chastity_Device).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Wrist_Restraints).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Wrist_Restraints).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        state(Toys.Chastity_Device).remove();

        assertFalse(state(Toys.Chastity_Device).applied());
        assertFalse(state(Locks.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Wrist_Restraints).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.CannotJerkOff).applied());

        assertTrue(state(Toys.Chastity_Device).expired());
        assertTrue(state(Locks.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Wrist_Restraints).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

    }
}
