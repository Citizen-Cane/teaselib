package teaselib.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import teaselib.Body;
import teaselib.State;
import teaselib.State.Persistence.Until;
import teaselib.Toys;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class StateScopeTests {

    private State somethingOnNipples;
    private TestScript script;
    private TeaseLib.PersistentString peerStorage;

    @BeforeEach
    public void beforeEach() throws IOException {
        script = new TestScript();
        script.teaseLib.freezeTime();
        somethingOnNipples = script.state(Body.OnNipples);
        peerStorage = script.teaseLib.getString(QualifiedName.of(TeaseLib.DefaultDomain,
                Body.class.getName() + "." + Body.OnNipples.name(), "state.peers"));
    }

    @AfterEach
    public void cleanup() {
        script.close();
    }

    @Test
    public void testLocalState() {
        assertThatByDefaultStateIsTemporary();
        assertThatStateIsPersisted();

        script.debugger.advanceTime(45, TimeUnit.MINUTES);
        Assertions.assertTrue(somethingOnNipples.expired());

        assertThatRemovedStateIsStillAvailableButNotPersisted();
    }

    @Test
    public void testLocalStateWhenRemovingBeforeExpired() {
        assertThatByDefaultStateIsTemporary();
        assertThatStateIsPersisted();

        Assertions.assertFalse(somethingOnNipples.expired());

        assertThatRemovedStateIsStillAvailableButNotPersisted();
    }

    private void assertThatByDefaultStateIsTemporary() {
        Assertions.assertFalse(somethingOnNipples.applied());
        Assertions.assertTrue(somethingOnNipples.expired());
        Assertions.assertFalse(peerStorage.available());
    }

    private void assertThatStateIsPersisted() {
        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES);
        Assertions.assertTrue(somethingOnNipples.applied());
        Assertions.assertFalse(peerStorage.available());

        somethingOnNipples.applyTo(Toys.Nipple_Clamps).over(30, TimeUnit.MINUTES).remember(Until.Removed);
        Assertions.assertTrue(peerStorage.available());
        String value = peerStorage.value();
        Assertions.assertTrue(value.contains(Toys.Nipple_Clamps.name()));
        Assertions.assertFalse(somethingOnNipples.expired());
    }

    private void assertThatRemovedStateIsStillAvailableButNotPersisted() {
        somethingOnNipples.remove();
        Assertions.assertFalse(somethingOnNipples.applied());
        Assertions.assertFalse(peerStorage.available());
    }

}
