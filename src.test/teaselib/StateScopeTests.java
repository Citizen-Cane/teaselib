package teaselib;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.test.TestScript;

public class StateScopeTests {

    @Test
    public void testLocalState() {
        TestScript script = TestScript.getOne();
        State somethingOnNipples = script.state(Body.SomethingOnNipples);

        assertInfiniteStateIsTemporary(script, somethingOnNipples);
        assertTimedStateIsPersisted(script, somethingOnNipples);
        assertRemovedStateIsNotPersistedAndExpired(script, somethingOnNipples);
    }

    private static void assertRemovedStateIsNotPersistedAndExpired(
            TestScript script, State state) {
        state.remove();
        assertFalse(state.applied());
        assertTrue(state.expired());
        assertEquals(null, script.teaseLib.getString(Body.class.getName(),
                Body.SomethingOnNipples.name()));
    }

    private static void assertTimedStateIsPersisted(TestScript script,
            State state) {
        state.apply(30, TimeUnit.MINUTES);
        assertTrue(state.applied());
        assertTrue(script.teaseLib.getString(Body.class.getName(),
                Body.SomethingOnNipples.name()) != null);
        assertFalse(state.expired());
    }

    private static State assertInfiniteStateIsTemporary(TestScript script,
            State state) {
        assertFalse(state.applied());
        assertTrue(state.expired());
        assertEquals(null, script.teaseLib.getString(Body.class.getName(),
                Body.SomethingOnNipples.name()));
        return state;
    }

}
