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

    public static final class PersistableImplementation<T> extends ArrayList<T> implements Persist.Persistable {
        private static final long serialVersionUID = 1L;

        private PersistableImplementation(List<T> values) {
            super(values);
        }

        public PersistableImplementation(Persist.Storage storage) {
            while (storage.hasNext()) {
                add(storage.next());
            }
        }

        @Override
        public List<String> persisted() {
            ArrayList<String> persistedElements = new ArrayList<>(size());
            for (Object value : this) {
                persistedElements.add(Persist.persist(value));
            }
            return persistedElements;
        }
    }

    @Test
    public void testStorage() throws Exception {
        List<Object> values = Arrays.asList(new Object[] { "Foo", 1, 2L, 2.7f, 3.14159, false, true });

        Persist.Persistable persistable = new PersistableImplementation<Object>(values);
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

    @Test
    public void testPersistRestore() throws Exception {
        List<Object> values = Arrays.asList(new Object[] { "Foo", 1, 2L, 2.7f, 3.14159, false, true });

        Persist.Persistable persistable = new PersistableImplementation<Object>(values);
        String persisted = Persist.persist(persistable);
        List<Object> restored = Persist.from(persisted);

        assertEquals(values, restored);
    }
}
