package teaselib;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class StateTests {
    final DummyPersistence persistence = new DummyPersistence();
    final TeaseLib teaseLib = new TeaseLib(new DummyHost(), persistence);
    final String namespace = "JUnit test";
    final TeaseScript script = new TeaseScript(teaseLib,
            new ResourceLoader(PersistenceTest.class),
            new Actor("Test", Voice.Gender.Female, "en-us"), namespace) {
        @Override
        public void run() {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void testLocalState() {
        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        assertFalse(somethingOnNipples.applied());
        somethingOnNipples.apply();
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnNipples.expired());
        somethingOnNipples.remove();
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
    }

    @Test
    public void testPersistentState() {
        State somethingOnNipples = script.state(Body.SomethingOnNipples);
        assertFalse(somethingOnNipples.applied());
        somethingOnNipples.apply(30, TimeUnit.MINUTES);
        assertTrue(somethingOnNipples.applied());
        assertFalse(somethingOnNipples.expired());
        assertEquals(30, somethingOnNipples.remaining(TimeUnit.MINUTES));
        somethingOnNipples.remove();
        assertTrue(somethingOnNipples.expired());
        assertFalse(somethingOnNipples.applied());
    }

}
