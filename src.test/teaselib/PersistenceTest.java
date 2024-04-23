/**
 * 
 */
package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.debug.DebugPersistence;
import teaselib.core.util.QualifiedName;
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
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(b.value() == false);
            b.set();
            assertEquals(DebugPersistence.TRUE,
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(b.available() == true);
            b.clear();
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(b.available() == false);
            b.set(false);
            assertEquals(DebugPersistence.FALSE,
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(b.available() == true);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
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
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            b.set(false);
            assertTrue(b.available() == true);
            assertEquals(DebugPersistence.FALSE,
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            b.set();
            assertEquals(DebugPersistence.TRUE,
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(b.available() == true);
            b.clear();
            assertTrue(b.value() == true);
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(b.available() == false);
            b.set();
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
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
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(f.value() == TeaseLib.PersistentFloat.DefaultValue);
            f.set(testValue);
            assertEquals(Double.toString(testValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(f.available() == true);
            f.clear();
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(f.available() == false);
            f.set(TeaseLib.PersistentFloat.DefaultValue);
            assertEquals(Double.toString(TeaseLib.PersistentFloat.DefaultValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(f.available() == true);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
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
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.set(testValue);
            assertTrue(f.available() == true);
            assertEquals(Double.toString(testValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.set(testDefaultValue);
            assertTrue(f.available() == true);
            assertEquals(Double.toString(testDefaultValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.clear();
            assertTrue(f.available() == false);
            assertTrue(f.value() == testDefaultValue);
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.set(testValue);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(f.available() == false);
        }
    }

    @Test
    public void testPersistentInteger() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestInteger1";
            long testValue = TeaseLib.PersistentNumber.DefaultValue + 1;
            TeaseLib.PersistentNumber i = script.persistence.newNumber(name);
            assertTrue(i.available() == false);
            assertEquals(TeaseLib.PersistentNumber.DefaultValue, i.value(), 0);
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.value() == TeaseLib.PersistentNumber.DefaultValue);
            i.set(testValue);
            assertEquals(Long.toString(testValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.available() == true);
            i.clear();
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.available() == false);
            i.set(TeaseLib.PersistentNumber.DefaultValue);
            assertEquals(Long.toString(TeaseLib.PersistentNumber.DefaultValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.available() == true);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            assertTrue(i.available() == false);
        }
    }

    @Test
    public void testPersistentIntegerDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestInteger2";
            long testValue = TeaseLib.PersistentNumber.DefaultValue + 1;
            long testDefaultValue = TeaseLib.PersistentNumber.DefaultValue + 2;
            TeaseLib.PersistentNumber i = script.persistence.newNumber(name).defaultValue(testDefaultValue);
            assertTrue(i.value() == testDefaultValue);
            assertTrue(i.available() == false);
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            assertTrue(i.available() == true);
            assertEquals(Long.toString(testValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testDefaultValue);
            assertTrue(i.available() == true);
            assertEquals(Long.toString(testDefaultValue),
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.clear();
            assertTrue(i.available() == false);
            assertTrue(i.value() == testDefaultValue);
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
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
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.value() == TeaseLib.PersistentString.DefaultValue);
            i.set(testValue);
            assertEquals(testValue, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.available() == true);
            i.clear();
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.available() == false);
            i.set(TeaseLib.PersistentString.DefaultValue);
            assertEquals(TeaseLib.PersistentString.DefaultValue,
                    script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            assertTrue(i.available() == true);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
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
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            assertTrue(i.available() == true);
            assertEquals(testValue, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testDefaultValue);
            assertTrue(i.available() == true);
            assertEquals(testDefaultValue, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.clear();
            assertTrue(i.available() == false);
            assertTrue(i.value() == testDefaultValue);
            assertEquals(null, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
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
