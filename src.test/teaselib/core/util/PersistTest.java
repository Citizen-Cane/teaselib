package teaselib.core.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class PersistTest {

    @Test
    public void testPersisted() throws Exception {
        String test = "foobar";
        String serialized = Persist.persist(test);

        assertEquals("Class=java.lang.String;Value=foobar", serialized);
    }

    @Test
    public void testPersistFromString() throws Exception {
        String serialized = "Class=java.lang.String;Value=foobar";
        String deserialized = Persist.from(serialized);

        assertEquals("foobar", deserialized);
    }

    @Test
    public void testPersistIntegralTypes() throws Exception {
        assertEquals("Foo", Persist.from(Persist.persist("Foo")));
        assertEquals(new Integer(1), Persist.from(Persist.persist(new Integer(1))));
        assertEquals(new Long(2), Persist.from(Persist.persist(new Long(2))));
        assertEquals(new Float(2.7), Persist.from(Persist.persist(new Float(2.7))));
        assertEquals(new Double(3.14159d), Persist.from(Persist.persist(new Double(3.14159d))));
        assertEquals(new Boolean(false), Persist.from(Persist.persist(new Boolean(false))));
        assertEquals(new Boolean(true), Persist.from(Persist.persist(new Boolean(true))));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPersistCollection() throws Exception {
        Collection<?> test = Arrays.asList("Foo", 1, 2L, 2.7f, 3.14159, false, true);
        assertEquals(test, Persist.from(Persist.persist(test)));
    }

    public final class PersistableImplementation implements Persist.Persistable {
        private final List<Object> values;

        private PersistableImplementation(List<Object> values) {
            this.values = values;
        }

        // TODO Write a test that uses the Storage constructor
        // public PersistableImplementation(Persist.Storage storage) {
        // this.values = new ArrayList<>();
        // }

        @Override
        public List<String> persisted() {
            ArrayList<String> persistedElements = new ArrayList<>(values.size());
            for (Object value : values) {
                persistedElements.add(Persist.persist(value));
            }
            return persistedElements;
        }
    }

    @Test
    public void testPersistViaInterface() throws Exception {
        Object[] array = { "Foo", 1, 2L, 2.7f, 3.14159, false, true };
        final List<Object> values = Arrays.asList(array);

        Persist.Persistable persistable = new PersistableImplementation(values);
        String persisted = Persist.persist(persistable);

        Persist.Storage storage = new Persist.Storage(Persist.persistedValue(persisted));

        assertEquals("Foo", storage.next());
        assertEquals(Integer.valueOf(1), storage.next());
        assertEquals(Long.valueOf(2L), storage.next());
        assertEquals(Float.valueOf(2.7f), storage.next());
        assertEquals(Double.valueOf(3.14159d), storage.next());
        assertEquals(false, storage.next());
        assertEquals(true, storage.next());

        storage = new Persist.Storage(Persist.persistedValue(persisted));
        List<Object> restored = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            restored.add(storage.next());
        }

        assertEquals(values, restored);
    }
}
