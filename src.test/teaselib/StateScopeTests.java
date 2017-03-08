package teaselib;

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

        TeaseLib.PersistentString stateStorage = script.teaseLib.new PersistentString(
                TeaseLib.DefaultDomain, Body.class.getName(),
                Body.SomethingOnNipples.name() + ".state");

        assertThatByDefaultStateIsTemporary(somethingOnNipples, stateStorage);
        assertThatTimedStateIsPersisted(somethingOnNipples, stateStorage);
        assertThatRemovedStateIsStillAvailableButNotPersisted(
                somethingOnNipples, stateStorage);
    }

    private static State assertThatByDefaultStateIsTemporary(State state,
            TeaseLib.PersistentString stateStorage) {
        assertFalse(state.applied());
        assertTrue(state.expired());
        assertFalse(stateStorage.available());
        return state;
    }

    private static void assertThatTimedStateIsPersisted(State state,
            TeaseLib.PersistentString stateStorage) {
        state.apply(Toys.Nipple_clamps, 30, TimeUnit.MINUTES);
        assertTrue(state.applied());
        assertFalse(stateStorage.available());

        state.remember();
        assertTrue(stateStorage.available());
        String value = stateStorage.value();
        assertTrue(value.contains(Toys.Nipple_clamps.name()));
        assertFalse(state.expired());
    }

    private static void assertThatRemovedStateIsStillAvailableButNotPersisted(
            State state, TeaseLib.PersistentString stateStorage) {
        state.remove();
        assertFalse(state.applied());
        assertTrue(state.expired());
        assertFalse(stateStorage.available());
    }

}
