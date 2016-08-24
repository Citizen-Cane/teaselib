/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

/**
 * @author someone
 *
 */
public class ToyPersistenceTests {
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

    @Before
    public void before() {
        persistence.storage.clear();
    }

    @Test
    public void testToysAndClothing() {
        teaseLib.getToy(Toys.Ball_Gag).setAvailable(true);
        assertTrue(persistence.storage.containsKey("toys.ball_gag"));
        teaseLib.getClothing(Clothing.Female, Clothing.High_Heels)
                .setAvailable(true);
        assertTrue(
                persistence.storage.containsKey("female.clothing.high_heels"));
    }

    @Test
    public void testToysAndClothingAsItems() {
        String namespace = "MyNameSpace";
        teaseLib.item(namespace, Toys.Ball_Gag).setAvailable(true);
        assertTrue(persistence.storage.containsKey(namespace.toLowerCase() + "."
                + Toys.class.getSimpleName().toLowerCase() + ".ball_gag"));
        teaseLib.item(namespace, Clothing.High_Heels).setAvailable(true);
        assertTrue(persistence.storage.containsKey(namespace.toLowerCase() + "."
                + Clothing.class.getSimpleName().toLowerCase()
                + ".high_heels"));
    }
}
