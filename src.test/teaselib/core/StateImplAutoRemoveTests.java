package teaselib.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.Duration;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.test.TestScript;

public class StateImplAutoRemoveTests {

    enum TestStates {
        TEST_STATE,
        BODY_PART
    }

    @Test
    public void testUntilExpiredRequiresDuration() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();
            State.Options options = script.state(TestStates.TEST_STATE).apply();
            assertThrows(IllegalArgumentException.class, () -> options.remember(Until.Expired));
        }
    }

    @Test
    public void testUntilRemovedDefaultDurationLimit() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();
            State state = script.state(TestStates.TEST_STATE);
            State.Options options = state.apply();
            options.remember(Until.Removed);
            assertEquals(Duration.INFINITE, state.duration().limit(TimeUnit.SECONDS));
        }
    }

    @Test
    public void testAutoRemovalAfterDuration() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();

            {
                State state = script.state(TestStates.TEST_STATE);
                State part = script.state(TestStates.BODY_PART);

                assertFalse(state.applied());
                assertTrue(state.expired());
                assertFalse(part.applied());
                assertTrue(part.expired());

                state.applyTo(TestStates.BODY_PART).over(2, TimeUnit.HOURS).remember(Until.Expired);

                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                State state = script.state(TestStates.TEST_STATE);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(1, TimeUnit.HOURS);
                script.triggerAutoRemove();
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                State state = script.state(TestStates.TEST_STATE);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(2, TimeUnit.HOURS);
                script.triggerAutoRemove();
                assertFalse(state.applied());
                assertTrue(state.expired());
                assertFalse(part.applied());
                assertTrue(part.expired());
            }
        }
    }

    @Test
    public void testAutoRemovalWithoutDuration() throws IOException {
        try (TestScript script = new TestScript()) {
            Debugger debugger = script.debugger;
            debugger.freezeTime();

            {
                State state = script.state(TestStates.TEST_STATE);
                State part = script.state(TestStates.BODY_PART);

                assertFalse(state.applied());
                assertTrue(state.expired());
                assertFalse(part.applied());
                assertTrue(part.expired());

                state.applyTo(TestStates.BODY_PART).remember(Until.Removed);
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                State state = script.state(TestStates.TEST_STATE);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(2, TimeUnit.HOURS);
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());
            }

            {
                debugger.clearStateMaps();

                State state = script.state(TestStates.TEST_STATE);
                State part = script.state(TestStates.BODY_PART);
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                script.triggerAutoRemove();
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

                debugger.advanceTime(64, TimeUnit.HOURS);
                script.triggerAutoRemove();
                assertTrue(state.applied());
                assertFalse(state.expired());
                assertTrue(part.applied());
                assertFalse(part.expired());

            }
        }
    }

}
