/**
 * 
 */
package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import teaselib.core.ResourceLoader;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

/**
 * @author someone
 *
 */
public class PersistenceTest {
    final static String namespace = "JUnit test";
    final static TeaseLib teaseLib = TeaseLib.init(new DummyHost(),
            new DummyPersistence());
    final static TeaseScript script = new TeaseScript(teaseLib,
            new ResourceLoader("x:/projects/teaseLib/", "bin"),
            new Actor(Actor.Dominant, Voice.Gender.Female, "en-us"),
            namespace) {
        @Override
        public void run() {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void testPersistentBoolean() {
        String name = "TestBoolean1";
        TeaseLib.PersistentBoolean b = script.persistentBoolean(name);
        assertTrue(b.available() == false);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(b.get() == false);
        b.set();
        assertEquals(DummyPersistence.True, script.getString(name));
        assertEquals(DummyPersistence.True,
                teaseLib.getString(namespace, name));
        assertTrue(b.available() == true);
        b.clear();
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(b.available() == false);
        b.set(false);
        assertEquals(DummyPersistence.False, script.getString(name));
        assertEquals(DummyPersistence.False,
                teaseLib.getString(namespace, name));
        assertTrue(b.available() == true);
        teaseLib.clear(namespace, name);
        assertTrue(b.available() == false);
    }

    @Test
    public void testPersistentBooleanDefault() {
        String name = "TestBoolean2";
        TeaseLib.PersistentBoolean b = script.persistentBoolean(name)
                .defaultValue(true);
        assertTrue(b.get() == true);
        assertTrue(b.available() == false);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        b.set(false);
        assertTrue(b.available() == true);
        assertEquals(DummyPersistence.False, script.getString(name));
        assertEquals(DummyPersistence.False,
                teaseLib.getString(namespace, name));
        b.set();
        assertEquals(DummyPersistence.True, script.getString(name));
        assertEquals(DummyPersistence.True,
                teaseLib.getString(namespace, name));
        assertTrue(b.available() == true);
        b.clear();
        assertTrue(b.get() == true);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(b.available() == false);
        b.set();
        teaseLib.clear(namespace, name);
        assertTrue(b.available() == false);
    }

    @Test
    public void testPersistentFloat() {
        String name = "TestFloat1";
        double testValue = TeaseLib.PersistentFloat.DefaultValue + 1.0;
        TeaseLib.PersistentFloat f = script.persistentFloat(name);
        assertTrue(f.available() == false);
        assertEquals(TeaseLib.PersistentFloat.DefaultValue, f.get(), 0.0);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(f.get() == TeaseLib.PersistentFloat.DefaultValue);
        f.set(testValue);
        assertEquals(Double.toString(testValue), script.getString(name));
        assertEquals(Double.toString(testValue),
                teaseLib.getString(namespace, name));
        assertTrue(f.available() == true);
        f.clear();
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(f.available() == false);
        f.set(TeaseLib.PersistentFloat.DefaultValue);
        assertEquals(Double.toString(TeaseLib.PersistentFloat.DefaultValue),
                script.getString(name));
        assertEquals(Double.toString(TeaseLib.PersistentFloat.DefaultValue),
                teaseLib.getString(namespace, name));
        assertTrue(f.available() == true);
        teaseLib.clear(namespace, name);
        assertTrue(f.available() == false);
    }

    @Test
    public void testPersistentFloatDefault() {
        String name = "TestFloat2";
        double testValue = TeaseLib.PersistentFloat.DefaultValue + 1.0;
        double testDefaultValue = TeaseLib.PersistentFloat.DefaultValue + 2.0;
        TeaseLib.PersistentFloat f = script.persistentFloat(name)
                .defaultValue(testDefaultValue);
        assertTrue(f.get() == testDefaultValue);
        assertTrue(f.available() == false);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        f.set(testValue);
        assertTrue(f.available() == true);
        assertEquals(Double.toString(testValue), script.getString(name));
        assertEquals(Double.toString(testValue),
                teaseLib.getString(namespace, name));
        f.set(testDefaultValue);
        assertTrue(f.available() == true);
        assertEquals(Double.toString(testDefaultValue), script.getString(name));
        assertEquals(Double.toString(testDefaultValue),
                teaseLib.getString(namespace, name));
        f.clear();
        assertTrue(f.available() == false);
        assertTrue(f.get() == testDefaultValue);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        f.set(testValue);
        teaseLib.clear(namespace, name);
        assertTrue(f.available() == false);
    }

    @Test
    public void testPersistentInteger() {
        String name = "TestInteger1";
        int testValue = TeaseLib.PersistentInteger.DefaultValue + 1;
        TeaseLib.PersistentInteger i = script.persistentInteger(name);
        assertTrue(i.available() == false);
        assertEquals(TeaseLib.PersistentInteger.DefaultValue, i.get(), 0);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(i.get() == TeaseLib.PersistentInteger.DefaultValue);
        i.set(testValue);
        assertEquals(Integer.toString(testValue), script.getString(name));
        assertEquals(Integer.toString(testValue),
                teaseLib.getString(namespace, name));
        assertTrue(i.available() == true);
        i.clear();
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(i.available() == false);
        i.set(TeaseLib.PersistentInteger.DefaultValue);
        assertEquals(Integer.toString(TeaseLib.PersistentInteger.DefaultValue),
                script.getString(name));
        assertEquals(Integer.toString(TeaseLib.PersistentInteger.DefaultValue),
                teaseLib.getString(namespace, name));
        assertTrue(i.available() == true);
        teaseLib.clear(namespace, name);
        assertTrue(i.available() == false);
    }

    @Test
    public void testPersistentIntegerDefault() {
        String name = "TestInteger2";
        int testValue = TeaseLib.PersistentInteger.DefaultValue + 1;
        int testDefaultValue = TeaseLib.PersistentInteger.DefaultValue + 2;
        TeaseLib.PersistentInteger i = script.persistentInteger(name)
                .defaultValue(testDefaultValue);
        assertTrue(i.get() == testDefaultValue);
        assertTrue(i.available() == false);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        i.set(testValue);
        assertTrue(i.available() == true);
        assertEquals(Integer.toString(testValue), script.getString(name));
        assertEquals(Integer.toString(testValue),
                teaseLib.getString(namespace, name));
        i.set(testDefaultValue);
        assertTrue(i.available() == true);
        assertEquals(Integer.toString(testDefaultValue),
                script.getString(name));
        assertEquals(Integer.toString(testDefaultValue),
                teaseLib.getString(namespace, name));
        i.clear();
        assertTrue(i.available() == false);
        assertTrue(i.get() == testDefaultValue);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        i.set(testValue);
        teaseLib.clear(namespace, name);
        assertTrue(i.available() == false);
    }

    @Test
    public void testPersistentString() {
        String name = "TestString1";
        String testValue = TeaseLib.PersistentString.DefaultValue + "test";
        TeaseLib.PersistentString i = script.persistentString(name);
        assertTrue(i.available() == false);
        assertEquals(TeaseLib.PersistentString.DefaultValue, i.get());
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(i.get() == TeaseLib.PersistentString.DefaultValue);
        i.set(testValue);
        assertEquals(testValue, script.getString(name));
        assertEquals(testValue, teaseLib.getString(namespace, name));
        assertTrue(i.available() == true);
        i.clear();
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        assertTrue(i.available() == false);
        i.set(TeaseLib.PersistentString.DefaultValue);
        assertEquals(TeaseLib.PersistentString.DefaultValue,
                script.getString(name));
        assertEquals(TeaseLib.PersistentString.DefaultValue,
                teaseLib.getString(namespace, name));
        assertTrue(i.available() == true);
        teaseLib.clear(namespace, name);
        assertTrue(i.available() == false);
    }

    @Test
    public void testPersistentStringDefault() {
        String name = "TestString2";
        String testValue = TeaseLib.PersistentString.DefaultValue + "test";
        String testDefaultValue = TeaseLib.PersistentString.DefaultValue + " 2";
        TeaseLib.PersistentString i = script.persistentString(name)
                .defaultValue(testDefaultValue);
        assertTrue(i.get() == testDefaultValue);
        assertTrue(i.available() == false);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        i.set(testValue);
        assertTrue(i.available() == true);
        assertEquals(testValue, script.getString(name));
        assertEquals(testValue, teaseLib.getString(namespace, name));
        i.set(testDefaultValue);
        assertTrue(i.available() == true);
        assertEquals(testDefaultValue, script.getString(name));
        assertEquals(testDefaultValue, teaseLib.getString(namespace, name));
        i.clear();
        assertTrue(i.available() == false);
        assertTrue(i.get() == testDefaultValue);
        assertEquals(null, script.getString(name));
        assertEquals(null, teaseLib.getString(namespace, name));
        i.set(testValue);
        teaseLib.clear(namespace, name);
        assertTrue(i.available() == false);
    }
}
