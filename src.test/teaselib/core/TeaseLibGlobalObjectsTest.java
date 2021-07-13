package teaselib.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import teaselib.core.configuration.DebugSetup;
import teaselib.core.debug.DebugHost;

public class TeaseLibGlobalObjectsTest {

    private final Map<Object, Object> globals = new HashMap<>();

    enum TestObject {
        OBJECT1,
        OBJECT2
    }

    static final TestObject key1 = TestObject.OBJECT1;
    static final String value1 = "Object1";

    static final TestObject key2 = TestObject.OBJECT2;
    static final Collection<?> value2 = Collections.emptyList();

    public <T> void storeGlobal(Object key, T value) {
        globals.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getGlobal(Object key) {
        return (T) globals.get(key);
    }

    @Test
    public void testGlobalObjects() {
        storeGlobal(TestObject.OBJECT1, value1);
        storeGlobal(TestObject.OBJECT2, value2);

        String retrieved1 = getGlobal(TestObject.OBJECT1);
        assertEquals(value1, retrieved1);

        Collection<?> retrieved2 = getGlobal(TestObject.OBJECT2);
        assertEquals(0, retrieved2.size());
        assertEquals(value2, retrieved2);
    }

    @Test
    public void testTeaseLibGLobals() throws IOException {
        try (TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugSetup());) {
            teaseLib.globals.store(TestObject.OBJECT1, value1);
            teaseLib.globals.store(TestObject.OBJECT2, value2);

            String retrieved1 = teaseLib.globals.get(TestObject.OBJECT1);
            assertEquals(value1, retrieved1);

            Collection<?> retrieved2 = teaseLib.globals.get(TestObject.OBJECT2);
            assertEquals(0, retrieved2.size());
            assertEquals(value2, retrieved2);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTeaseLibGlobalObjectsAreWriteOnceToEnsureIntegrityAndOwnership() throws IOException {
        try (TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugSetup());) {
            teaseLib.globals.store(TestObject.OBJECT1, value1);
            teaseLib.globals.store(TestObject.OBJECT1, value2);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTeaseLibGlobalObjectSuppliersAreWriteOnceToEnsureIntegrityAndOwnership() throws IOException {
        try (TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugSetup());) {
            teaseLib.globals.store(TestObject.OBJECT1, () -> value1);
            teaseLib.globals.store(TestObject.OBJECT1, () -> value2);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTeaseLibGlobalObjectSuppliersAreWriteOnceToEnsureIntegrityAndOwnership2() throws IOException {
        try (TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugSetup());) {
            teaseLib.globals.store(TestObject.OBJECT1, () -> value1);
            teaseLib.globals.store(TestObject.OBJECT1, value2);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTeaseLibGlobalObjectSuppliersAreWriteOnceToEnsureIntegrityAndOwnership3() throws IOException {
        try (TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugSetup());) {
            teaseLib.globals.store(TestObject.OBJECT1, () -> value1);
            teaseLib.globals.store(TestObject.OBJECT1, value2);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTeaseLibGlobalObjectSuppliersAreWriteOnceToEnsureIntegrityAndOwnership4() throws IOException {
        try (TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugSetup());) {
            teaseLib.globals.store(TestObject.OBJECT1, value1);
            teaseLib.globals.store(TestObject.OBJECT1, () -> value2);
        }
    }
}
