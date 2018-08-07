package teaselib.core;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.State;
import teaselib.test.TestScript;

public class StateImplAutoRemoveTests {

    enum TestStates {
        TEST_STATE,
        BODY_PART
    }

    @Test
    public void testAutoRemoval() {
        TestScript script = TestScript.getOne();
        Debugger debugger = script.debugger;
        debugger.freezeTime();

        State state = script.state(TestStates.TEST_STATE);
        State part = script.state(TestStates.BODY_PART);

        assertFalse(state.applied());
        assertTrue(state.expired());

        state.applyTo(TestStates.BODY_PART).over(2, TimeUnit.HOURS);
        // TODO Resolve : body part is temporary but should be duration
        assertApplied(state, part);
        assertPersisted(debugger, state, part);
        assertExpired(debugger, state, part);
        assertAutoRemoved(debugger, state, part);
    }

    private static void assertApplied(State state, State part) {
        assertTrue(state.applied());
        assertFalse(state.expired());
        assertTrue(part.applied());
        assertFalse(part.expired());
    }

    private static void assertPersisted(Debugger debugger, State state, State part) {
        debugger.clearStateMaps();
        assertTrue(state.applied());
        assertFalse(state.expired());
        assertTrue(part.applied());
        assertFalse(part.expired());
    }

    private static void assertExpired(Debugger debugger, State state, State part) {
        debugger.clearStateMaps();
        debugger.advanceTime(2, TimeUnit.HOURS);
        assertTrue(state.applied());
        assertTrue(state.expired());
        assertTrue(part.applied());
        assertTrue(part.expired());
    }

    private static void assertAutoRemoved(Debugger debugger, State state, State part) {
        debugger.clearStateMaps();
        debugger.advanceTime(2, TimeUnit.HOURS);
        assertFalse(state.applied());
        assertTrue(state.expired());
        assertFalse(part.applied());
        assertTrue(part.expired());
    }
}
