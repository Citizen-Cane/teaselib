package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import teaselib.State;
import teaselib.hosts.DummyPersistence;
import teaselib.test.TestScript;

@RunWith(Parameterized.class)
public class StateMapsPersistenceTest extends StateMaps {
    public static final String TEST_DOMAIN = "test";

    enum Locks {
        Chastity_Device_Lock
    }

    enum NestedTestToys {
        Chastity_Device,
        Wrist_Restraints
    }

    enum NestedTestBody {
        SomethingOnPenis,
        CannotJerkOff,
        WristsTiedBehindBack
    }

    final TestScript script;
    final DummyPersistence persistence;
    final TestParameter rememberState;

    enum TestParameter {
        DontTestPersistence,
        TestPersistence
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays
                .asList(new Object[][] { { TestParameter.DontTestPersistence }, { TestParameter.TestPersistence } });
    }

    public StateMapsPersistenceTest(TestParameter remember) {
        this(TestScript.getOne(), remember);
    }

    StateMapsPersistenceTest(TestScript script, TestParameter remember) {
        super(script.teaseLib);
        this.script = script;
        persistence = script.persistence;
        rememberState = remember;

        script.teaseLib.freezeTime();
    }

    @Before
    public void initStorage() {
        persistence.storage.clear();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
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

    @Test
    public void testPersistenceOnLock() {
        rememberOrNot(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).apply(NestedTestBody.SomethingOnPenis,
                NestedTestBody.CannotJerkOff));

        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(NestedTestToys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(NestedTestToys.Chastity_Device).over(24,
                TimeUnit.HOURS));

        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        clearStatesMapsOrNot();

        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());

        state(TEST_DOMAIN, NestedTestToys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());

        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
    }

    @Test
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBack() {
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());

        state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).apply(NestedTestBody.WristsTiedBehindBack,
                NestedTestBody.CannotJerkOff);
        rememberOrNot(state(TEST_DOMAIN, NestedTestToys.Chastity_Device)
                .apply(NestedTestBody.SomethingOnPenis, NestedTestBody.CannotJerkOff).over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());

        state(TEST_DOMAIN, NestedTestToys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
    }

    @Test
    public void testCannotJerkOffWhenWearingALockedChastityCageAndHandsTiedOnBack() {
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());

        rememberOrNot(state(TEST_DOMAIN, NestedTestToys.Chastity_Device)
                .apply(NestedTestBody.SomethingOnPenis, NestedTestBody.CannotJerkOff)
                .over(State.INDEFINITELY, TimeUnit.HOURS));

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).apply(NestedTestToys.Chastity_Device).over(24,
                TimeUnit.HOURS));

        clearStatesMapsOrNot();

        teaseLib.advanceTime(22, TimeUnit.HOURS);

        state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).apply(NestedTestBody.WristsTiedBehindBack,
                NestedTestBody.CannotJerkOff);

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).remove();

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        teaseLib.advanceTime(1, TimeUnit.HOURS);

        // cage is applied indefinitely
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        state(TEST_DOMAIN, NestedTestToys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());
    }

    @Test
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBackWithStrings() {
        String Toys_Chastity_Device = "teaselib.Toys.Chastity_Device";
        String Toys_Wrist_Restraints = "teaselib.Toys.Wrist_Restraints";

        String Body_WristsTiedBehindBack = "teaselib.Body.WristsTiedBehindBack";
        String Body_SomethingOnPenis = "teaselib.Body.SomethingOnPenis";
        String Body_CannotJerkOff = "teaselib.Body.CannotJerkOff";

        assertFalse(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());

        state(TEST_DOMAIN, Toys_Wrist_Restraints).apply(Body_WristsTiedBehindBack, Body_CannotJerkOff);
        rememberOrNot(state(TEST_DOMAIN, Toys_Chastity_Device).apply(Body_SomethingOnPenis, Body_CannotJerkOff).over(24,
                TimeUnit.HOURS));

        clearStatesMapsOrNot();

        assertTrue(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body_SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body_CannotJerkOff).applied());

        assertEquals(!isRemembered(), state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());

        if (isRemembered()) {
            Map<String, String> storage = script.persistence.storage;
            assertEquals(6, storage.size());
            // The teaselib package names are stripped from names of persisted
            // items, so it's just Toys.*
            assertTrue(storage.containsKey(TEST_DOMAIN + "." + stripPath(Toys_Chastity_Device) + ".state.duration"));
            assertTrue(storage.containsKey(TEST_DOMAIN + "." + stripPath(Toys_Chastity_Device) + ".state.peers"));
            assertTrue(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_SomethingOnPenis) + ".state.duration"));
            assertTrue(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_SomethingOnPenis) + ".state.peers"));
            assertTrue(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_CannotJerkOff) + ".state.duration"));
            assertTrue(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_CannotJerkOff) + ".state.peers"));
        }

        state(TEST_DOMAIN, Toys_Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Body_SomethingOnPenis).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, Body_CannotJerkOff).applied());
        // wrists still tied behind back -> cannot jerk off

        assertEquals(!isRemembered(), state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, Body_CannotJerkOff).applied());

        if (isRemembered()) {
            Map<String, String> storage = script.persistence.storage;
            assertEquals(0, storage.size());
            // The teaselib package names are stripped from names of persisted
            // items, so it's just Toys.*
            assertFalse(storage.containsKey(TEST_DOMAIN + "." + stripPath(Toys_Chastity_Device) + ".state.duration"));
            assertFalse(storage.containsKey(TEST_DOMAIN + "." + stripPath(Toys_Chastity_Device) + ".state.peers"));
            assertFalse(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_SomethingOnPenis) + ".state.duration"));
            assertFalse(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_SomethingOnPenis) + ".state.peers"));
            assertFalse(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_CannotJerkOff) + ".state.duration"));
            assertFalse(storage.containsKey(TEST_DOMAIN + "." + stripPath(Body_CannotJerkOff) + ".state.peers"));
        }

        state(TEST_DOMAIN, Toys_Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());
    }

    private String stripPath(String name) {
        return persistence.getNameMapping().stripPath(null, name, null);
    }

}
