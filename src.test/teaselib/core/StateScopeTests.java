package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.test.TestScript;

public class StateScopeTests {

    private State somethingOnNipples;
    private TestScript script;
    private TeaseLib.PersistentString peerStorage;

    @Before
    public void before() {
        script = TestScript.getOne();
        script.teaseLib.freezeTime();
        somethingOnNipples = script.state(Body.OnNipples);
        peerStorage = script.teaseLib.new PersistentString(TeaseLib.DefaultDomain,
                Body.class.getName() + "." + Body.OnNipples.name(), "state.peers");
    }

    @Test
    public void testLocalState() {
        assertThatByDefaultStateIsTemporary();
        assertThatStateIsPersisted();

        script.debugger.advanceTime(45, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.expired());

        assertThatRemovedStateIsStillAvailableButNotPersisted();
    }

    @Test
    public void testLocalStateWhenRemovingBeforeExpired() {
        assertThatByDefaultStateIsTemporary();
        assertThatStateIsPersisted();

        assertFalse(somethingOnNipples.expired());

        assertThatRemovedStateIsStillAvailableButNotPersisted();
    }

    private void assertThatByDefaultStateIsTemporary() {
        assertFalse(somethingOnNipples.applied());
        assertTrue(somethingOnNipples.expired());
        assertFalse(peerStorage.available());
    }

    private void assertThatStateIsPersisted() {
        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(peerStorage.available());

        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES).remember(Until.Removed);
        assertTrue(peerStorage.available());
        String value = peerStorage.value();
        assertTrue(value.contains(Toys.Nipple_Clamps.name()));
        assertFalse(somethingOnNipples.expired());
    }

    private void assertThatRemovedStateIsStillAvailableButNotPersisted() {
        somethingOnNipples.remove();
        assertFalse(somethingOnNipples.applied());
        assertFalse(peerStorage.available());
    }

}
