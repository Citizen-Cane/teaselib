package teaselib.core.util;

import static org.junit.Assert.*;

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
        assertEquals(Integer.valueOf(1), Persist.from(Persist.persist(Integer.valueOf(1))));
        assertEquals(Long.valueOf(2), Persist.from(Persist.persist(Long.valueOf(2))));
        assertEquals(Float.valueOf(2.7f), Persist.from(Persist.persist(Float.valueOf(2.7f))));
        assertEquals(Double.valueOf(3.14159d), Persist.from(Persist.persist(Double.valueOf(3.14159d))));
        assertEquals(Boolean.valueOf(false), Persist.from(Persist.persist(Boolean.valueOf(false))));
        assertEquals(Boolean.valueOf(true), Persist.from(Persist.persist(Boolean.valueOf(true))));
    }

    @Test
    public void testPersistCollection() throws Exception {
        Collection<?> test = Arrays.asList("Foo", 1, 2L, 2.7f, 3.14159, false, true);
        String persisted = Persist.persist(test);
        Collection<?> restored = Persist.from(persisted);
        assertEquals(test, restored);
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

        Persist.Persistable persistable = new PersistableImplementation<>(values);
        String persisted = Persist.persist(persistable);

        Persist.Storage storage = new Persist.Storage(Persist.persistedValue(persisted));

        assertEquals("Foo", storage.next());
        assertEquals(Integer.valueOf(1), storage.next());
        assertEquals(Long.valueOf(2L), storage.next());
        assertEquals(Float.valueOf(2.7f), storage.next());
        assertEquals(Double.valueOf(3.14159d), storage.next());
        assertEquals(false, storage.next());
        assertEquals(true, storage.next());
        assertFalse(storage.hasNext());

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

        Persist.Persistable persistable = new PersistableImplementation<>(values);
        String persisted = Persist.persist(persistable);
        List<Object> restored = Persist.from(persisted);

        assertEquals(values, restored);
    }
}
