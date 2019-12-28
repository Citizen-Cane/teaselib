package teaselib.core;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;
import static teaselib.core.StateMapsPersistenceTest.NestedTestBody.*;
import static teaselib.core.StateMapsPersistenceTest.NestedTestToys.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Body;
import teaselib.Duration;
import teaselib.State;
import teaselib.Toys;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

@RunWith(Parameterized.class)
public class StateMapsPersistenceTest extends StateMaps {
    private static final Logger logger = LoggerFactory.getLogger(StateMapsPersistenceTest.class);

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
    final Persistence persistence;
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
        script.storage.clear();

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
            script.storage.printTo(logger);
            clear();
        }
    }

    private boolean isRemembered() {
        return rememberState == TestParameter.TestPersistence;
    }

    @Test
    public void testPersistenceOnLock() {
        rememberOrNot(state(TEST_DOMAIN, Chastity_Device).applyTo(SomethingOnPenis, CannotJerkOff));

        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(NestedTestToys.Chastity_Device);

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Chastity_Device).over(24, TimeUnit.HOURS));

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

        state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applyTo(NestedTestBody.WristsTiedBehindBack,
                NestedTestBody.CannotJerkOff);
        rememberOrNot(
                state(TEST_DOMAIN, Chastity_Device).applyTo(SomethingOnPenis, CannotJerkOff).over(24, TimeUnit.HOURS));

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

        rememberOrNot(state(TEST_DOMAIN, Chastity_Device).applyTo(SomethingOnPenis, CannotJerkOff)
                .over(State.INDEFINITELY, TimeUnit.HOURS));

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Chastity_Device).over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        teaseLib.advanceTime(22, TimeUnit.HOURS);

        state(TEST_DOMAIN, Wrist_Restraints).applyTo(WristsTiedBehindBack, CannotJerkOff);

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

        // TODO expire on removal, allow to query last duration
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
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

        state(TEST_DOMAIN, Toys_Wrist_Restraints).applyTo(Body_WristsTiedBehindBack, Body_CannotJerkOff);
        rememberOrNot(state(TEST_DOMAIN, Toys_Chastity_Device).applyTo(Body_SomethingOnPenis, Body_CannotJerkOff)
                .over(24, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        assertTrue(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body_SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body_CannotJerkOff).applied());

        assertEquals(!isRemembered(), state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());

        if (isRemembered()) {
            Map<QualifiedName, String> storage = script.storage;
            assertEquals(9, storage.size()); // or 6
            // The teaselib package names are stripped from names of persisted
            // items, so it's just Toys.*
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Toys", "Chastity_Device.state.duration")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Toys", "Chastity_Device.state.peers")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body", "SomethingOnPenis.state.duration")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body", "SomethingOnPenis.state.peers")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body", "CannotJerkOff.state.duration")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body", "CannotJerkOff.state.peers")));
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
            Map<QualifiedName, String> storage = script.storage;
            assertEquals(0, storage.size());
            // The teaselib package names are stripped from names of persisted
            // items, so it's just Toys.*
            assertFalse(
                    storage.containsKey(QualifiedName.of(TEST_DOMAIN, Toys_Chastity_Device + ".state", "duration")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Toys_Chastity_Device + ".state", "peers")));
            assertFalse(
                    storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_SomethingOnPenis + ".state", "duration")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_SomethingOnPenis + ".state", "peers")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_CannotJerkOff + ".state", "duration")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_CannotJerkOff + ".state", "peers")));
        }

        state(TEST_DOMAIN, Toys_Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());
    }

    @Test
    public void testPersistenceOfDuratioElapsedOfRemovedItems() {
        rememberOrNot(state(TEST_DOMAIN, Toys.Enema_Kit).apply().over(1, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        if (isRemembered()) {
            Map<QualifiedName, String> storage = script.storage;
            assertEquals(2, storage.size());
        }

        assertTrue(state(TEST_DOMAIN, Toys.Enema_Kit).applied());

        // TODO did remember after remove(), should remember removal automatically
        state(TEST_DOMAIN, Toys.Enema_Kit).remove();

        if (isRemembered()) {
            Map<QualifiedName, String> storage = script.storage;
            assertEquals("State not cleared on remove", 0, storage.size());
        }

        assertFalse(state(TEST_DOMAIN, Toys.Enema_Kit).applied());

        // False because we removed the item early
        // TODO make it true, since expiration is meaningless after remove
        assertFalse(state(TEST_DOMAIN, Toys.Enema_Kit).expired());
        script.debugger.advanceTime(1, HOURS);
        assertTrue(state(TEST_DOMAIN, Toys.Enema_Kit).expired());

        clearStatesMapsOrNot();

        teaseLib.advanceTime(23, TimeUnit.HOURS);
        Duration duration = state(TEST_DOMAIN, Toys.Enema_Kit).duration();
        if (isRemembered()) {
            assertEquals(0, duration.start(DAYS));
        } else {
            assertEquals(86400, duration.elapsed(TimeUnit.SECONDS));
            assertEquals(24, duration.elapsed(TimeUnit.HOURS));
            assertEquals(1, duration.elapsed(TimeUnit.DAYS));
        }
    }

    @Test
    public void testPersistenceOfElapsedDurationOfRemovedStateWithPeers() {
        rememberOrNot(state(TEST_DOMAIN, Toys.Ball_Stretcher).applyTo(Body.OnBalls).over(1, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        if (isRemembered()) {
            assertEquals(6, script.storage.size());
        }

        assertTrue(state(TEST_DOMAIN, Toys.Ball_Stretcher).applied());

        state(TEST_DOMAIN, Toys.Ball_Stretcher).removeFrom(Body.OnBalls);

        assertEquals("Expected item to be completely cleared", 0, script.storage.size());

        assertFalse(state(TEST_DOMAIN, Toys.Ball_Stretcher).applied());
        // False because we removed the item early
        assertFalse(state(TEST_DOMAIN, Toys.Ball_Stretcher).expired());
        script.debugger.advanceTime(1, HOURS);
        assertTrue(state(TEST_DOMAIN, Toys.Ball_Stretcher).expired());

        clearStatesMapsOrNot();
        teaseLib.advanceTime(23, TimeUnit.HOURS);

        Duration duration = state(TEST_DOMAIN, Toys.Ball_Stretcher).duration();
        // TODO Should always be the same last used duration
        if (isRemembered()) {
            assertEquals(0, duration.start(DAYS));
        } else {
            assertEquals(86400, duration.elapsed(TimeUnit.SECONDS));
            assertEquals(24, duration.elapsed(TimeUnit.HOURS));
            assertEquals(1, duration.elapsed(TimeUnit.DAYS));
        }
    }

}
