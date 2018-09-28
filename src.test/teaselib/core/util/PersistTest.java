package teaselib.core.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import teaselib.core.util.Persist.PersistedObject;

public class PersistTest {
    @Test
    public void testPersisted() throws Exception {
        String test = "Foo";
        String serialized = Persist.persist(test);

        assertEquals("Class=java.lang.String;Value=Foo", serialized);
    }

    @Test
    public void testPersistFromString() throws Exception {
        String serialized = "Class=java.lang.String;Value=Foo";
        String deserialized = Persist.from(serialized);

        assertEquals("Foo", deserialized);
    }

    @Test
    public void testPersistToString() throws Exception {
        String persisted = Persist.persist("Foo");
        assertEquals("Class=java.lang.String;Value=Foo", persisted);
    }

    enum Foo {
        Bar
    }

    @Test
    public void testPersistFromEnum() throws Exception {
        String serialized = "Class=teaselib.core.util.PersistTest$Foo;Value=Bar";
        Foo deserialized = Persist.from(serialized);

        assertEquals(Foo.Bar, deserialized);
    }

    @Test
    public void testPersistToEnum() throws Exception {
        String persisted = Persist.persist(Foo.Bar);
        assertEquals("Class=teaselib.core.util.PersistTest$Foo;Value=Bar", persisted);
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
    public void testPersistableImplementation() throws Exception {
        List<Object> values = Arrays.asList(new Object[] { "Foo", 1, 2L, 2.7f, 3.14159, false, true });

        Persist.Persistable persistable = new PersistableImplementation<>(values);
        String persisted = Persist.persist(persistable);

        Persist.Storage storage = new PersistedObject(persisted).toStorage();

        assertEquals("Foo", storage.next());
        assertEquals(Integer.valueOf(1), storage.next());
        assertEquals(Long.valueOf(2L), storage.next());
        assertEquals(Float.valueOf(2.7f), storage.next());
        assertEquals(Double.valueOf(3.14159d), storage.next());
        assertEquals(false, storage.next());
        assertEquals(true, storage.next());
        assertFalse(storage.hasNext());

        storage = new PersistedObject(persisted).toStorage();
        List<Object> restored = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            restored.add(storage.next());
        }

        assertEquals(values, restored);
    }

    public static final class NamedArrayList implements Persist.Persistable {
        final String name;
        final List<Object> values;

        NamedArrayList(String name, Object... values) {
            this.name = name;
            this.values = Arrays.asList(values);
        }

        public NamedArrayList(Persist.Storage storage) {
            this.name = storage.next();
            this.values = storage.next();
            assertFalse(storage.hasNext());
        }

        @Override
        public List<String> persisted() {
            return Arrays.stream(new Object[] { name, values }).map(Persist::persist).collect(Collectors.toList());
        }
    }

    @Test
    public void testPersistableImplementationNested() throws Exception {
        Object[] values = { "Foo", 1, 2L, 2.7f, 3.14159, false, true };

        NamedArrayList persistable = new NamedArrayList("Test", values);
        String persisted = Persist.persist(persistable);

        NamedArrayList restored = Persist.from(persisted);
        assertEquals(persistable.name, restored.name);
        assertEquals(persistable.values, restored.values);
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
