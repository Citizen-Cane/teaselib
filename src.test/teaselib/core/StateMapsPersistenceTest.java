package teaselib.core;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;
import static teaselib.core.StateMapsPersistenceTest.NestedTestBody.*;
import static teaselib.core.StateMapsPersistenceTest.NestedTestToys.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Body;
import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

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

    public enum TestParameter {
        DontTestPersistence,
        TestPersistence
    }

    public StateMapsPersistenceTest() throws IOException {
        super(new TestScript());
        script.teaseLib.freezeTime();
    }

    @BeforeEach
    public void initStorage() {
        script.storage.clear();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
    }

    @AfterEach
    public void cleanup() {
        script.close();
    }

    void rememberOrNot(State.Persistence state, TestParameter rememberState) {
        if (isRemembered(rememberState)) {
            state.remember(Until.Removed);
        }
    }

    void clearStatesMapsOrNot(TestParameter rememberState) {
        if (isRemembered(rememberState)) {
            script.storage.printTo(logger);
            clear();
        }
    }

    private boolean isRemembered(TestParameter rememberState) {
        return rememberState == TestParameter.TestPersistence;
    }

    @ParameterizedTest
    @EnumSource(TestParameter.class)
    public void testPersistenceOnLock(TestParameter rememberState) {
        rememberOrNot(state(TEST_DOMAIN, Chastity_Device).applyTo(SomethingOnPenis, CannotJerkOff), rememberState);

        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(NestedTestToys.Chastity_Device);

        boolean notRemembered = !isRemembered(rememberState);
        assertEquals(notRemembered, state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());
        assertEquals(notRemembered, state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applyTo(Chastity_Device).over(24, TimeUnit.HOURS), rememberState);

        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());
        assertEquals(notRemembered, state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertEquals(notRemembered, state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        clearStatesMapsOrNot(rememberState);

        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());
        assertEquals(notRemembered, state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).expired());
        assertEquals(notRemembered, state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).expired());

        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());

        teaseLib.advanceTime(24, TimeUnit.HOURS);

        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertEquals(notRemembered, state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());

        State chastityDevice = state(TEST_DOMAIN, NestedTestToys.Chastity_Device);
        state(TEST_DOMAIN, Locks.Chastity_Device_Lock).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertTrue(chastityDevice.applied());
        assertEquals(notRemembered, chastityDevice.expired());

        state(TEST_DOMAIN, NestedTestToys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).applied());
        assertTrue(state(TEST_DOMAIN, Locks.Chastity_Device_Lock).expired());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertEquals(notRemembered, state(TEST_DOMAIN, NestedTestToys.Chastity_Device).expired());

        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
    }

    @ParameterizedTest
    @EnumSource(TestParameter.class)
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBack(TestParameter rememberState) {
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());

        state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applyTo(NestedTestBody.WristsTiedBehindBack,
                NestedTestBody.CannotJerkOff);
        rememberOrNot(
                state(TEST_DOMAIN, Chastity_Device).applyTo(SomethingOnPenis, CannotJerkOff).over(24, TimeUnit.HOURS),rememberState);

        clearStatesMapsOrNot(rememberState);

        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).is(NestedTestBody.SomethingOnPenis));
        assertTrue(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).is(NestedTestBody.CannotJerkOff));
        assertTrue(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertEquals(!isRemembered(rememberState),
                state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).is(NestedTestBody.CannotJerkOff));
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());

        state(TEST_DOMAIN, NestedTestToys.Chastity_Device).remove();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.SomethingOnPenis).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, NestedTestBody.CannotJerkOff).applied());

        state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).remove();

        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestBody.WristsTiedBehindBack).applied());
    }

    @ParameterizedTest
    @EnumSource(TestParameter.class)
    public void testCannotJerkOffWhenWearingALockedChastityCageAndHandsTiedOnBack(TestParameter rememberState) {
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, NestedTestToys.Wrist_Restraints).applied());

        rememberOrNot(state(TEST_DOMAIN, Chastity_Device)
                .applyTo(SomethingOnPenis, CannotJerkOff)
                .over(Duration.INFINITE, TimeUnit.HOURS), rememberState);

        rememberOrNot(state(TEST_DOMAIN, Locks.Chastity_Device_Lock)
                .applyTo(Chastity_Device)
                .over(24, TimeUnit.HOURS), rememberState);

        clearStatesMapsOrNot(rememberState);

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

    @ParameterizedTest
    @EnumSource(TestParameter.class)
    public void testCannotJerkOffWearingAChastityCageAndHandsTiedOnBackWithStrings(TestParameter rememberState) {
        String Toys_Chastity_Device = "teaselib.Toys.Chastity_Device";
        String Toys_Wrist_Restraints = "teaselib.Bondage.Wrist_Restraints";

        String Body_WristsTiedBehindBack = "teaselib.Body.WristsTiedBehindBack";
        String Body_SomethingOnPenis = "teaselib.Body.SomethingOnPenis";
        String Body_CannotJerkOff = "teaselib.Body.CannotJerkOff";

        assertFalse(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertFalse(state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());

        state(TEST_DOMAIN, Toys_Wrist_Restraints).applyTo(Body_WristsTiedBehindBack, Body_CannotJerkOff);
        rememberOrNot(state(TEST_DOMAIN, Toys_Chastity_Device)
                .applyTo(Body_SomethingOnPenis, Body_CannotJerkOff)
                .over(24, TimeUnit.HOURS), rememberState);

        clearStatesMapsOrNot(rememberState);

        assertTrue(state(TEST_DOMAIN, Toys_Chastity_Device).applied());
        assertTrue(state(TEST_DOMAIN, Body_SomethingOnPenis).applied());
        assertTrue(state(TEST_DOMAIN, Body_CannotJerkOff).applied());

        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());

        if (isRemembered(rememberState)) {
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

        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, Body_CannotJerkOff).applied());
        // wrists still tied behind back -> cannot jerk off
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, Toys_Wrist_Restraints).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, Body_WristsTiedBehindBack).applied());
        assertEquals(!isRemembered(rememberState), state(TEST_DOMAIN, Body_CannotJerkOff).applied());

        if (isRemembered(rememberState)) {
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

    @ParameterizedTest
    @EnumSource(TestParameter.class)
    public void testPersistenceOfDuratioElapsedOfRemovedItems(TestParameter rememberState) {
        rememberOrNot(state(TEST_DOMAIN, Toys.Enema_Kit)
                .apply()
                .over(1, TimeUnit.HOURS), rememberState);

        clearStatesMapsOrNot(rememberState);

        if (isRemembered(rememberState)) {
            assertEquals(9, script.storageSize());
        }

        assertTrue(state(TEST_DOMAIN, Toys.Enema_Kit).applied());

        // TODO did remember after remove(), should remember removal automatically
        script.debugger.advanceTime(1, TimeUnit.MINUTES);
        state(TEST_DOMAIN, Toys.Enema_Kit).remove();

        if (isRemembered(rememberState)) {
            assertEquals(3, script.storageSize(),
                    "State not cleared on remove (excluding auto-removal book-keeping)");
        }

        assertFalse(state(TEST_DOMAIN, Toys.Enema_Kit).applied());

        // False because we removed the item early
        assertFalse(state(TEST_DOMAIN, Toys.Enema_Kit).expired());
        script.debugger.advanceTime(1, HOURS);
        assertTrue(state(TEST_DOMAIN, Toys.Enema_Kit).expired());

        clearStatesMapsOrNot(rememberState);

        Duration duration = state(TEST_DOMAIN, Toys.Enema_Kit).duration();
        assertEquals(60, duration.limit(MINUTES));
        assertEquals(1, duration.elapsed(MINUTES));
        teaseLib.advanceTime(23, HOURS);
        assertEquals(1, duration.elapsed(MINUTES));
    }

    @ParameterizedTest
    @EnumSource(TestParameter.class)
    public void testPersistenceOfElapsedDurationOfRemovedStateWithPeers(TestParameter rememberState) {
        rememberOrNot(state(TEST_DOMAIN, Toys.Ball_Stretcher).applyTo(Body.OnBalls).over(2, TimeUnit.HOURS), rememberState);

        clearStatesMapsOrNot(rememberState);

        if (isRemembered(rememberState)) {
            assertEquals(12, script.storageSize());
        }

        assertTrue(state(TEST_DOMAIN, Toys.Ball_Stretcher).applied());

        teaseLib.advanceTime(1, TimeUnit.HOURS);
        state(TEST_DOMAIN, Toys.Ball_Stretcher).removeFrom(Body.OnBalls);
        State ballStretcher = state(TEST_DOMAIN, Toys.Ball_Stretcher);
        State onBalls = state(TEST_DOMAIN, Body.OnBalls);
        assertFalse(onBalls.applied());
        assertEquals(isRemembered(rememberState) ? 3 : 0,
                script.storageSize(),
                "State not completely cleared (excluding auto-removal book-keeping)");
        assertFalse(ballStretcher.applied());

        // False because we removed the item early
        assertFalse(state(TEST_DOMAIN, Toys.Ball_Stretcher).expired());
        assertEquals(1, state(TEST_DOMAIN, Toys.Ball_Stretcher).duration().elapsed(HOURS));

        teaseLib.advanceTime(1, TimeUnit.HOURS);
        assertTrue(state(TEST_DOMAIN, Toys.Ball_Stretcher).expired());
        assertEquals(1, state(TEST_DOMAIN, Toys.Ball_Stretcher).duration().elapsed(HOURS));

        clearStatesMapsOrNot(rememberState);
        teaseLib.advanceTime(22, TimeUnit.HOURS);

        Duration duration = state(TEST_DOMAIN, Toys.Ball_Stretcher).duration();
        assertEquals(3600, duration.elapsed(TimeUnit.SECONDS));
        assertEquals(1, duration.elapsed(TimeUnit.HOURS));
        assertEquals(0, duration.elapsed(DAYS));
    }

}
