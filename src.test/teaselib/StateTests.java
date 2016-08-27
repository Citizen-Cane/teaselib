package teaselib;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class StateTests {
    final static DummyPersistence persistence = new DummyPersistence();
    final static String namespace = "JUnit test";
    final static TeaseLib teaseLib = TeaseLib.init(new DummyHost(),
            persistence);
    final static TeaseScript script = new TeaseScript(teaseLib,
            new ResourceLoader(PersistenceTest.class),
            new Actor("Test", Voice.Gender.Female, "en-us"), namespace) {
        @Override
        public void run() {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void testLocalState() {
        State<Body>.Item item = script.state(Body.SomethingOnNipples);
        assertFalse(item.applied());
        item.apply();
        assertTrue(item.applied());
        assertFalse(item.expired());
        item.remove();
        assertTrue(item.expired());
        assertFalse(item.applied());
    }

    @Test
    public void testPersistentState() {
        State<Body>.Item item = script.state(Body.SomethingOnNipples);
        assertFalse(item.applied());
        item.apply(30, TimeUnit.MINUTES);
        assertTrue(item.applied());
        assertFalse(item.expired());
        assertEquals(30, item.remaining(TimeUnit.MINUTES));
        item.remove();
        assertTrue(item.expired());
        assertFalse(item.applied());
    }

}
