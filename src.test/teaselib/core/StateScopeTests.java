package teaselib.core;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.Toys;
import teaselib.test.TestScript;

public class StateScopeTests {
    private State somethingOnNipples;
    private TeaseLib.PersistentString peerStorage;

    @Before
    public void before() {
        TestScript script = TestScript.getOne();
        script.teaseLib.freezeTime();

        somethingOnNipples = script.state(Body.OnNipples);
        peerStorage = script.teaseLib.new PersistentString(TeaseLib.DefaultDomain, Body.class.getName(),
                Body.OnNipples.name() + ".state.peers");
    }

    @Test
    public void testLocalState() {
        assertThatByDefaultStateIsTemporary();
        assertThatStateIsPersisted();
        assertThatRemovedStateIsStillAvailableButNotPersisted();
    }

    private void assertThatByDefaultStateIsTemporary() {
        assertFalse(somethingOnNipples.applied());
        assertTrue(somethingOnNipples.expired());
        assertFalse(peerStorage.available());
    }

    private void assertThatStateIsPersisted() {
        somethingOnNipples.applyTo(Toys.Nipple_Clamps);
        assertTrue(somethingOnNipples.applied());
        assertFalse(peerStorage.available());

        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertTrue(peerStorage.available());
        String value = peerStorage.value();
        assertTrue(value.contains(Toys.Nipple_Clamps.name()));
        assertFalse(somethingOnNipples.expired());
    }

    private void assertThatRemovedStateIsStillAvailableButNotPersisted() {
        somethingOnNipples.remove();
        assertFalse(somethingOnNipples.applied());
        assertTrue(somethingOnNipples.expired());
        assertFalse(peerStorage.available());
    }
}
