package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.test.TestScript;

public class StateScopeTests {

    @Test
    public void testLocalState() {
        TestScript script = TestScript.getOne();
        State somethingOnNipples = script.state(Body.SomethingOnNipples);

        assertThatInfiniteStateIsTemporary(script, somethingOnNipples);
        assertThatTimedStateIsPersisted(script, somethingOnNipples);
        assertThatRemovedStateIsStillPersistedButExpired(script,
                somethingOnNipples);
    }

    private static State assertThatInfiniteStateIsTemporary(TestScript script,
            State state) {
        assertFalse(state.applied());
        assertTrue(state.expired());
        assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain,
                Body.class.getName(), Body.SomethingOnNipples.name()));
        return state;
    }

    private static void assertThatRemovedStateIsStillPersistedButExpired(
            TestScript script, State state) {
        state.remove();
        assertFalse(state.applied());
        assertTrue(state.expired());
        assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain,
                Body.class.getName(), Body.SomethingOnNipples.name()));
    }

    private static void assertThatTimedStateIsPersisted(TestScript script,
            State state) {
        state.apply(30, TimeUnit.MINUTES);
        assertTrue(state.applied());
        assertFalse(script.teaseLib.getString(TeaseLib.DefaultDomain,
                Body.class.getName(),
                Body.SomethingOnNipples.name() + ".state") != null);
        state.remember();
        assertTrue(script.teaseLib.getString(TeaseLib.DefaultDomain,
                Body.class.getName(),
                Body.SomethingOnNipples.name() + ".state") != null);

        assertFalse(state.expired());
    }

}
