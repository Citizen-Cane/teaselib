/**
 * 
 */
package teaselib;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.debug.DebugPersistence;
import teaselib.test.TestScript;

/**
 * @author someone
 *
 */
public class PersistenceTest {

    @Test
    public void testPersistentBoolean() throws IOException {
        try (TestScript script = new TestScript()) {
            String name = "TestBoolean1";
            TeaseLib.PersistentBoolean b = script.persistence.newBoolean(name);
            assertTrue(b.available() == false);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(b.value() == false);
            b.set();
            assertEquals(DebugPersistence.TRUE, script.persistence.getString(name));
            assertEquals(DebugPersistence.TRUE,
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(b.available() == true);
            b.clear();
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(b.available() == false);
            b.set(false);
            assertEquals(DebugPersistence.FALSE, script.persistence.getString(name));
            assertEquals(DebugPersistence.FALSE,
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(b.available() == true);
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(b.available() == false);
        }
    }

    @Test
    public void testPersistentBooleanDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestBoolean2";
            TeaseLib.PersistentBoolean b = script.persistence.newBoolean(name).defaultValue(true);
            assertTrue(b.value() == true);
            assertTrue(b.available() == false);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            b.set(false);
            assertTrue(b.available() == true);
            assertEquals(DebugPersistence.FALSE, script.persistence.getString(name));
            assertEquals(DebugPersistence.FALSE,
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            b.set();
            assertEquals(DebugPersistence.TRUE, script.persistence.getString(name));
            assertEquals(DebugPersistence.TRUE,
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(b.available() == true);
            b.clear();
            assertTrue(b.value() == true);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(b.available() == false);
            b.set();
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(b.available() == false);
        }
    }

    @Test
    public void testPersistentFloat() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestFloat1";
            double testValue = TeaseLib.PersistentFloat.DefaultValue + 1.0;
            TeaseLib.PersistentFloat f = script.persistence.newFloat(name);
            assertTrue(f.available() == false);
            assertEquals(TeaseLib.PersistentFloat.DefaultValue, f.value(), 0.0);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(f.value() == TeaseLib.PersistentFloat.DefaultValue);
            f.set(testValue);
            assertEquals(Double.toString(testValue), script.persistence.getString(name));
            assertEquals(Double.toString(testValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(f.available() == true);
            f.clear();
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(f.available() == false);
            f.set(TeaseLib.PersistentFloat.DefaultValue);
            assertEquals(Double.toString(TeaseLib.PersistentFloat.DefaultValue), script.persistence.getString(name));
            assertEquals(Double.toString(TeaseLib.PersistentFloat.DefaultValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(f.available() == true);
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(f.available() == false);
        }
    }

    @Test
    public void testPersistentFloatDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestFloat2";
            double testValue = TeaseLib.PersistentFloat.DefaultValue + 1.0;
            double testDefaultValue = TeaseLib.PersistentFloat.DefaultValue + 2.0;
            TeaseLib.PersistentFloat f = script.persistence.newFloat(name).defaultValue(testDefaultValue);
            assertTrue(f.value() == testDefaultValue);
            assertTrue(f.available() == false);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            f.set(testValue);
            assertTrue(f.available() == true);
            assertEquals(Double.toString(testValue), script.persistence.getString(name));
            assertEquals(Double.toString(testValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            f.set(testDefaultValue);
            assertTrue(f.available() == true);
            assertEquals(Double.toString(testDefaultValue), script.persistence.getString(name));
            assertEquals(Double.toString(testDefaultValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            f.clear();
            assertTrue(f.available() == false);
            assertTrue(f.value() == testDefaultValue);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            f.set(testValue);
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(f.available() == false);
        }
    }

    @Test
    public void testPersistentInteger() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestInteger1";
            int testValue = TeaseLib.PersistentInteger.DefaultValue + 1;
            TeaseLib.PersistentInteger i = script.persistence.newInteger(name);
            assertTrue(i.available() == false);
            assertEquals(TeaseLib.PersistentInteger.DefaultValue, i.value(), 0);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.value() == TeaseLib.PersistentInteger.DefaultValue);
            i.set(testValue);
            assertEquals(Integer.toString(testValue), script.persistence.getString(name));
            assertEquals(Integer.toString(testValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.available() == true);
            i.clear();
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.available() == false);
            i.set(TeaseLib.PersistentInteger.DefaultValue);
            assertEquals(Integer.toString(TeaseLib.PersistentInteger.DefaultValue), script.persistence.getString(name));
            assertEquals(Integer.toString(TeaseLib.PersistentInteger.DefaultValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.available() == true);
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(i.available() == false);
        }
    }

    @Test
    public void testPersistentIntegerDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestInteger2";
            int testValue = TeaseLib.PersistentInteger.DefaultValue + 1;
            int testDefaultValue = TeaseLib.PersistentInteger.DefaultValue + 2;
            TeaseLib.PersistentInteger i = script.persistence.newInteger(name).defaultValue(testDefaultValue);
            assertTrue(i.value() == testDefaultValue);
            assertTrue(i.available() == false);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.set(testValue);
            assertTrue(i.available() == true);
            assertEquals(Integer.toString(testValue), script.persistence.getString(name));
            assertEquals(Integer.toString(testValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.set(testDefaultValue);
            assertTrue(i.available() == true);
            assertEquals(Integer.toString(testDefaultValue), script.persistence.getString(name));
            assertEquals(Integer.toString(testDefaultValue),
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.clear();
            assertTrue(i.available() == false);
            assertTrue(i.value() == testDefaultValue);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.set(testValue);
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(i.available() == false);
        }
    }

    @Test
    public void testPersistentString() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestString1";
            String testValue = TeaseLib.PersistentString.DefaultValue + "test";
            TeaseLib.PersistentString i = script.persistence.newString(name);
            assertTrue(i.available() == false);
            assertEquals(TeaseLib.PersistentString.DefaultValue, i.value());
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.value() == TeaseLib.PersistentString.DefaultValue);
            i.set(testValue);
            assertEquals(testValue, script.persistence.getString(name));
            assertEquals(testValue, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.available() == true);
            i.clear();
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.available() == false);
            i.set(TeaseLib.PersistentString.DefaultValue);
            assertEquals(TeaseLib.PersistentString.DefaultValue, script.persistence.getString(name));
            assertEquals(TeaseLib.PersistentString.DefaultValue,
                    script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.available() == true);
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(i.available() == false);
        }
    }

    @Test
    public void testPersistentStringDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestString2";
            String testValue = TeaseLib.PersistentString.DefaultValue + "test";
            String testDefaultValue = TeaseLib.PersistentString.DefaultValue + " 2";
            TeaseLib.PersistentString i = script.persistence.newString(name).defaultValue(testDefaultValue);
            assertTrue(i.value() == testDefaultValue);
            assertTrue(i.available() == false);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.set(testValue);
            assertTrue(i.available() == true);
            assertEquals(testValue, script.persistence.getString(name));
            assertEquals(testValue, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.set(testDefaultValue);
            assertTrue(i.available() == true);
            assertEquals(testDefaultValue, script.persistence.getString(name));
            assertEquals(testDefaultValue, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.clear();
            assertTrue(i.available() == false);
            assertTrue(i.value() == testDefaultValue);
            assertEquals(null, script.persistence.getString(name));
            assertEquals(null, script.teaseLib.getString(TeaseLib.DefaultDomain, script.namespace, name));
            i.set(testValue);
            script.teaseLib.clear(TeaseLib.DefaultDomain, script.namespace, name);
            assertTrue(i.available() == false);
        }
    }

    enum Fruit {
        Apple,
        Banana
    }

    @Test
    public void testPersistentEnum() throws IOException {
        try (TestScript script = new TestScript()) {

            PersistentEnum<Fruit> myFruit = script.persistence.newEnum("myfruit", Fruit.class).defaultValue(Fruit.Banana);
            assertEquals(Fruit.Banana, myFruit.value());
            myFruit.set(Fruit.Apple);
            assertEquals(Fruit.Apple, myFruit.value());
        }
    }
}
