package teaselib.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import teaselib.core.debug.DebugHost;
import teaselib.core.debug.DebugPersistence;
import teaselib.test.DebugSetup;

public class TeaseLibGlobalObjectsTest {

    private final Map<Object, Object> globals = new HashMap<>();

    enum TestObject {
        Object1,
        Object2
    }

    final TestObject key1 = TestObject.Object1;
    final String value1 = new String("Object1");

    final TestObject key2 = TestObject.Object2;
    final Collection<?> value2 = Collections.EMPTY_LIST;

    public <T> void storeGlobal(Object key, T value) {
        globals.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getGlobal(Object key) {
        return (T) globals.get(key);
    }

    @Test
    public void testGlobalObjects() {
        storeGlobal(TestObject.Object1, value1);
        storeGlobal(TestObject.Object2, value2);

        String retrieved1 = getGlobal(TestObject.Object1);
        assertEquals(value1, retrieved1);

        Collection<?> retrieved2 = getGlobal(TestObject.Object2);
        assertEquals(0, retrieved2.size());
        assertEquals(value2, retrieved2);
    }

    @Test
    public void testTeaseLibGLobals() throws IOException {
        TeaseLib teaseLib = new TeaseLib(new DebugHost(), new DebugPersistence(), new DebugSetup());

        teaseLib.globals.store(TestObject.Object1, value1);
        teaseLib.globals.store(TestObject.Object2, value2);

        String retrieved1 = teaseLib.globals.get(TestObject.Object1);
        assertEquals(value1, retrieved1);

        Collection<?> retrieved2 = teaseLib.globals.get(TestObject.Object2);
        assertEquals(0, retrieved2.size());
        assertEquals(value2, retrieved2);
    }

}
