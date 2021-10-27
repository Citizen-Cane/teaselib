package teaselib.core;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;
import static teaselib.core.StateMapsPersistenceTest.NestedTestBody.*;
import static teaselib.core.StateMapsPersistenceTest.NestedTestToys.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
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
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

@RunWith(Parameterized.class)
public class StateMapsPersistenceTest extends TestableStateMaps {
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

    public StateMapsPersistenceTest(TestParameter remember) throws IOException {
        this(new TestScript(), remember);
    }

    StateMapsPersistenceTest(TestScript script, TestParameter remember) {
        super(script.teaseLib);
        this.script = script;
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

    @After
    public void cleanup() {
        script.close();
    }

    void rememberOrNot(State.Persistence state) {
        if (isRemembered()) {
            state.remember(Until.Removed);
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

        State chastityDevice = state(TEST_DOMAIN, NestedTestToys.Chastity_Device);
        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(chastityDevice.applied());
        assertTrue(chastityDevice.expired());

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
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).is(NestedTestBody.SomethingOnPenis));
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).is(NestedTestBody.CannotJerkOff));
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertEquals(!isRemembered(),
                state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).is(NestedTestBody.CannotJerkOff));
        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());

        state(TEST_DOMAIN, NestedTestToys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertEquals(!isRemembered(), state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

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
                .over(Duration.INFINITE, TimeUnit.HOURS));

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
            assertEquals(15, script.storageSize());
            // The teaselib package names are stripped from names of persisted
            // items, so it's just Toys.*
            Map<QualifiedName, String> storage = script.storage;
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Toys.Chastity_Device", "state.duration")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Toys.Chastity_Device", "state.peers")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body.SomethingOnPenis", "state.duration")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body.SomethingOnPenis", "state.peers")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body.CannotJerkOff", "state.duration")));
            assertTrue(storage.containsKey(QualifiedName.of(TEST_DOMAIN, "Body.CannotJerkOff", "state.peers")));
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
            assertEquals(3, script.storageSize());
            // The teaselib package names are stripped from names of persisted
            // items, so it's just Toys.*
            Map<QualifiedName, String> storage = script.storage;
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Toys_Chastity_Device, "state.duration")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Toys_Chastity_Device, "state.peers")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_SomethingOnPenis, "state.duration")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_SomethingOnPenis, "state.peers")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_CannotJerkOff, "state.duration")));
            assertFalse(storage.containsKey(QualifiedName.of(TEST_DOMAIN, Body_CannotJerkOff, "state.peers")));
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
            assertEquals(9, script.storageSize());
        }

        assertTrue(state(TEST_DOMAIN, Toys.Enema_Kit).applied());

        // TODO did remember after remove(), should remember removal automatically
        script.debugger.advanceTime(1, TimeUnit.MINUTES);
        state(TEST_DOMAIN, Toys.Enema_Kit).remove();

        if (isRemembered()) {
            assertEquals("State not cleared on remove (excluding auto-removal book-keeping)", 3, script.storageSize());
        }

        assertFalse(state(TEST_DOMAIN, Toys.Enema_Kit).applied());

        // False because we removed the item early
        assertFalse(state(TEST_DOMAIN, Toys.Enema_Kit).expired());
        script.debugger.advanceTime(1, HOURS);
        assertTrue(state(TEST_DOMAIN, Toys.Enema_Kit).expired());

        clearStatesMapsOrNot();

        Duration duration = state(TEST_DOMAIN, Toys.Enema_Kit).duration();
        assertEquals(60, duration.limit(MINUTES));
        assertEquals(1, duration.elapsed(MINUTES));
        teaseLib.advanceTime(23, HOURS);
        assertEquals(1, duration.elapsed(MINUTES));
    }

    @Test
    public void testPersistenceOfElapsedDurationOfRemovedStateWithPeers() {
        rememberOrNot(state(TEST_DOMAIN, Toys.Ball_Stretcher).applyTo(Body.OnBalls).over(2, TimeUnit.HOURS));

        clearStatesMapsOrNot();

        if (isRemembered()) {
            assertEquals(12, script.storageSize());
        }

        assertTrue(state(TEST_DOMAIN, Toys.Ball_Stretcher).applied());

        teaseLib.advanceTime(1, TimeUnit.HOURS);
        state(TEST_DOMAIN, Toys.Ball_Stretcher).removeFrom(Body.OnBalls);
        State ballStretcher = state(TEST_DOMAIN, Toys.Ball_Stretcher);
        State onBalls = state(TEST_DOMAIN, Body.OnBalls);
        assertFalse(onBalls.applied());
        assertEquals("State not completely cleared (excluding auto-removal book-keeping)", isRemembered() ? 3 : 0,
                script.storageSize());
        assertFalse(ballStretcher.applied());

        // False because we removed the item early
        assertFalse(state(TEST_DOMAIN, Toys.Ball_Stretcher).expired());
        assertEquals(1, state(TEST_DOMAIN, Toys.Ball_Stretcher).duration().elapsed(HOURS));

        teaseLib.advanceTime(1, TimeUnit.HOURS);
        assertTrue(state(TEST_DOMAIN, Toys.Ball_Stretcher).expired());
        assertEquals(1, state(TEST_DOMAIN, Toys.Ball_Stretcher).duration().elapsed(HOURS));

        clearStatesMapsOrNot();
        teaseLib.advanceTime(22, TimeUnit.HOURS);

        Duration duration = state(TEST_DOMAIN, Toys.Ball_Stretcher).duration();
        assertEquals(3600, duration.elapsed(TimeUnit.SECONDS));
        assertEquals(1, duration.elapsed(TimeUnit.HOURS));
        assertEquals(0, duration.elapsed(DAYS));
    }

}
