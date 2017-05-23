package teaselib.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import teaselib.Body;
import teaselib.State;
import teaselib.Toys;
import teaselib.core.TeaseLib;
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
        peerStorage = script.teaseLib.new PersistentString(
                TeaseLib.DefaultDomain, Body.class.getName(),
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
        somethingOnNipples.apply(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(peerStorage.available());

        somethingOnNipples.apply(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES)
                .remember();
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
