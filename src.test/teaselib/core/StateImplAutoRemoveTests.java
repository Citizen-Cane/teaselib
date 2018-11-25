package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.State;
import teaselib.TeaseScript;
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
        assertFalse(part.applied());
        assertTrue(part.expired());

        state.applyTo(TestStates.BODY_PART).over(2, TimeUnit.HOURS);

        assertApplied(script);
        assertPersisted(script, debugger);
        assertExpired(script, debugger);
        assertPersistedAndRestored(script, debugger);
        assertAutoRemoved(script, debugger);
    }

    private static void assertApplied(TeaseScript script) {
        State state = script.state(TestStates.TEST_STATE);
        State part = script.state(TestStates.BODY_PART);

        assertTrue(state.applied());
        assertFalse(state.expired());
        assertTrue(part.applied());
        assertFalse(part.expired());
    }

    private static void assertPersisted(TeaseScript script, Debugger debugger) {
        debugger.clearStateMaps();

        State state = script.state(TestStates.TEST_STATE);
        State part = script.state(TestStates.BODY_PART);

        assertTrue(state.applied());
        assertFalse(state.expired());
        assertTrue(part.applied());
        assertFalse(part.expired());
    }

    private static void assertExpired(TeaseScript script, Debugger debugger) {
        debugger.advanceTime(2, TimeUnit.HOURS);

        State state = script.state(TestStates.TEST_STATE);
        State part = script.state(TestStates.BODY_PART);

        assertTrue(state.applied());
        assertTrue(state.expired());
        assertTrue(part.applied());
        assertTrue(part.expired());
    }

    private static void assertPersistedAndRestored(TeaseScript script, Debugger debugger) {
        debugger.clearStateMaps();

        State state = script.state(TestStates.TEST_STATE);
        State part = script.state(TestStates.BODY_PART);

        assertTrue(state.applied());
        assertTrue(state.expired());
        assertTrue(part.applied());
        assertTrue(part.expired());
    }

    private static void assertAutoRemoved(TeaseScript script, Debugger debugger) {
        debugger.advanceTime(2, TimeUnit.HOURS);
        debugger.clearStateMaps();

        State state = script.state(TestStates.TEST_STATE);
        State part = script.state(TestStates.BODY_PART);

        assertFalse(state.applied());
        assertTrue(state.expired());
        assertFalse(part.applied());
        assertTrue(part.expired());
    }
}
