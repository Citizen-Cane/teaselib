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
        return Arrays
                .asList(new Object[][] { { TestParameter.DontTestPersistence },
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

        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());
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
        rememberOrNot(state(Toys.Chastity_Cage).apply(Body.SomethingOnPenis,
                Body.CannotJerkOff));

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage);

        assertTrue(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());

        rememberOrNot(state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage)
                .over(24, TimeUnit.HOURS));

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

        clearStatesMapsOrNot();

        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).expired());
        assertFalse(state(Toys.Chastity_Cage).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Device_Lock).remove();

        assertFalse(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());

        state(Toys.Chastity_Cage).remove();

        assertFalse(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Cage).expired());

        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
    }

    @Test
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBack() {
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());

        state(Toys.Leather_Wrist_Cuffs).apply(Body.WristsTiedBehindBack,
                Body.CannotJerkOff);
        rememberOrNot(state(Toys.Chastity_Cage)
                .apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertEquals(isTemporary(), state(Toys.Leather_Wrist_Cuffs).applied());
        assertEquals(isTemporary(), state(Body.WristsTiedBehindBack).applied());

        state(Toys.Chastity_Cage).remove();

        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertEquals(isTemporary(), state(Toys.Leather_Wrist_Cuffs).applied());
        assertEquals(isTemporary(), state(Body.WristsTiedBehindBack).applied());

        state(Toys.Leather_Wrist_Cuffs).remove();

        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
    }

    @Test
    public void testCannotJerkOffWhenWearingALockedChastityCageAndHandsTiedOnBack() {
        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());

        rememberOrNot(state(Toys.Chastity_Cage)
                .apply(Body.SomethingOnPenis, Body.CannotJerkOff)
                .over(State.INDEFINITELY, TimeUnit.HOURS));

        rememberOrNot(state(Toys.Chastity_Device_Lock).apply(Toys.Chastity_Cage)
                .over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        teaseLib.advanceTime(22, TimeUnit.HOURS);

        state(Toys.Leather_Wrist_Cuffs).apply(Body.WristsTiedBehindBack,
                Body.CannotJerkOff);

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertTrue(state(Toys.Leather_Wrist_Cuffs).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Chastity_Cage).expired());
        assertFalse(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Leather_Wrist_Cuffs).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        state(Toys.Leather_Wrist_Cuffs).remove();

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertTrue(state(Toys.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Chastity_Cage).expired());
        assertFalse(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Leather_Wrist_Cuffs).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        // cage is applied indefinitely
        assertFalse(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Leather_Wrist_Cuffs).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        state(Toys.Chastity_Device_Lock).remove();

        assertTrue(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
        assertTrue(state(Body.SomethingOnPenis).applied());
        assertTrue(state(Body.CannotJerkOff).applied());

        assertFalse(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Leather_Wrist_Cuffs).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertFalse(state(Body.SomethingOnPenis).expired());
        assertFalse(state(Body.CannotJerkOff).expired());

        state(Toys.Chastity_Cage).remove();

        assertFalse(state(Toys.Chastity_Cage).applied());
        assertFalse(state(Toys.Chastity_Device_Lock).applied());
        assertFalse(state(Toys.Leather_Wrist_Cuffs).applied());
        assertFalse(state(Body.WristsTiedBehindBack).applied());
        assertFalse(state(Body.SomethingOnPenis).applied());
        assertFalse(state(Body.CannotJerkOff).applied());

        assertTrue(state(Toys.Chastity_Cage).expired());
        assertTrue(state(Toys.Chastity_Device_Lock).expired());
        assertTrue(state(Toys.Leather_Wrist_Cuffs).expired());
        assertTrue(state(Body.WristsTiedBehindBack).expired());
        assertTrue(state(Body.SomethingOnPenis).expired());
        assertTrue(state(Body.CannotJerkOff).expired());

    }
}
