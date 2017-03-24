package teaselib;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.test.TestScript;

public class StateScopeTests {

    private State somethingOnNipples;
    private TestScript script;
    private TeaseLib.PersistentString peerStorage;

    @Before
    public void before() {
        script = TestScript.getOne();
        somethingOnNipples = script.state(Body.SomethingOnNipples);
        peerStorage = script.teaseLib.new PersistentString(
                TeaseLib.DefaultDomain, Body.class.getName(),
                Body.SomethingOnNipples.name() + ".state.peers");
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
        somethingOnNipples.apply(Toys.Nipple_clamps).over(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(peerStorage.available());

        somethingOnNipples.apply(Toys.Nipple_clamps).over(30, TimeUnit.MINUTES)
                .remember();
        assertTrue(peerStorage.available());
        String value = peerStorage.value();
        assertTrue(value.contains(Toys.Nipple_clamps.name()));
        assertFalse(somethingOnNipples.expired());
    }

    private void assertThatRemovedStateIsStillAvailableButNotPersisted() {
        somethingOnNipples.remove();
        assertFalse(somethingOnNipples.applied());
        assertTrue(somethingOnNipples.expired());
        assertFalse(peerStorage.available());
    }

}
