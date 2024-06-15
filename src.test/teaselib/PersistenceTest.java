/**
 *
 */
package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.core.debug.DebugPersistence;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

import java.io.IOException;

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
            Assertions.assertFalse(b.available());
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertFalse(b.value());
            b.set();
            Assertions.assertEquals(DebugPersistence.TRUE, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(b.available());
            b.clear();
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertFalse(b.available());
            b.set(false);
            Assertions.assertEquals(DebugPersistence.FALSE, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(b.available());
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(b.available());
        }
    }

    @Test
    public void testPersistentBooleanDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestBoolean2";
            TeaseLib.PersistentBoolean b = script.persistence.newBoolean(name).defaultValue(true);
            Assertions.assertTrue(b.value());
            Assertions.assertFalse(b.available());
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            b.set(false);
            Assertions.assertTrue(b.available());
            Assertions.assertEquals(DebugPersistence.FALSE, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            b.set();
            Assertions.assertEquals(DebugPersistence.TRUE, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(b.available());
            b.clear();
            Assertions.assertTrue(b.value());
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertFalse(b.available());
            b.set();
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(b.available());
        }
    }

    @Test
    public void testPersistentFloat() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestFloat1";
            double testValue = TeaseLib.PersistentFloat.DefaultValue + 1.0;
            TeaseLib.PersistentFloat f = script.persistence.newFloat(name);
            Assertions.assertFalse(f.available());
            Assertions.assertEquals(TeaseLib.PersistentFloat.DefaultValue, f.value(), 0.0);
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertEquals(TeaseLib.PersistentFloat.DefaultValue, f.value());
            f.set(testValue);
            Assertions.assertEquals(Double.toString(testValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(f.available());
            f.clear();
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertFalse(f.available());
            f.set(TeaseLib.PersistentFloat.DefaultValue);
            Assertions.assertEquals(Double.toString(TeaseLib.PersistentFloat.DefaultValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(f.available());
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(f.available());
        }
    }

    @Test
    public void testPersistentFloatDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestFloat2";
            double testValue = TeaseLib.PersistentFloat.DefaultValue + 1.0;
            double testDefaultValue = TeaseLib.PersistentFloat.DefaultValue + 2.0;
            TeaseLib.PersistentFloat f = script.persistence.newFloat(name).defaultValue(testDefaultValue);
            Assertions.assertEquals(f.value(), testDefaultValue);
            Assertions.assertFalse(f.available());
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.set(testValue);
            Assertions.assertTrue(f.available());
            Assertions.assertEquals(Double.toString(testValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.set(testDefaultValue);
            Assertions.assertTrue(f.available());
            Assertions.assertEquals(Double.toString(testDefaultValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.clear();
            Assertions.assertFalse(f.available());
            Assertions.assertEquals(f.value(), testDefaultValue);
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            f.set(testValue);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(f.available());
        }
    }

    @Test
    public void testPersistentInteger() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestInteger1";
            long testValue = TeaseLib.PersistentNumber.DefaultValue + 1;
            TeaseLib.PersistentNumber i = script.persistence.newNumber(name);
            Assertions.assertFalse(i.available());
            Assertions.assertEquals(TeaseLib.PersistentNumber.DefaultValue, i.value(), 0);
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertEquals(TeaseLib.PersistentNumber.DefaultValue, (long) i.value());
            i.set(testValue);
            Assertions.assertEquals(Long.toString(testValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(i.available());
            i.clear();
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertFalse(i.available());
            i.set(TeaseLib.PersistentNumber.DefaultValue);
            Assertions.assertEquals(Long.toString(TeaseLib.PersistentNumber.DefaultValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(i.available());
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(i.available());
        }
    }

    @Test
    public void testPersistentIntegerDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestInteger2";
            long testValue = TeaseLib.PersistentNumber.DefaultValue + 1;
            long testDefaultValue = TeaseLib.PersistentNumber.DefaultValue + 2;
            TeaseLib.PersistentNumber i = script.persistence.newNumber(name).defaultValue(testDefaultValue);
            Assertions.assertEquals((long) i.value(), testDefaultValue);
            Assertions.assertFalse(i.available());
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            Assertions.assertTrue(i.available());
            Assertions.assertEquals(Long.toString(testValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testDefaultValue);
            Assertions.assertTrue(i.available());
            Assertions.assertEquals(Long.toString(testDefaultValue), script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.clear();
            Assertions.assertFalse(i.available());
            Assertions.assertEquals((long) i.value(), testDefaultValue);
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(i.available());
        }
    }

    @Test
    public void testPersistentString() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestString1";
            String testValue = TeaseLib.PersistentString.DefaultValue + "test";
            TeaseLib.PersistentString i = script.persistence.newString(name);
            Assertions.assertFalse(i.available());
            Assertions.assertEquals(TeaseLib.PersistentString.DefaultValue, i.value());
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertSame(TeaseLib.PersistentString.DefaultValue, i.value());
            i.set(testValue);
            Assertions.assertEquals(testValue, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(i.available());
            i.clear();
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertFalse(i.available());
            i.set(TeaseLib.PersistentString.DefaultValue);
            Assertions.assertEquals(TeaseLib.PersistentString.DefaultValue, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            Assertions.assertTrue(i.available());
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(i.available());
        }
    }

    @Test
    public void testPersistentStringDefault() throws IOException {
        try (TestScript script = new TestScript()) {

            String name = "TestString2";
            String testValue = TeaseLib.PersistentString.DefaultValue + "test";
            String testDefaultValue = TeaseLib.PersistentString.DefaultValue + " 2";
            TeaseLib.PersistentString i = script.persistence.newString(name).defaultValue(testDefaultValue);
            Assertions.assertSame(i.value(), testDefaultValue);
            Assertions.assertFalse(i.available());
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            Assertions.assertTrue(i.available());
            Assertions.assertEquals(testValue, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testDefaultValue);
            Assertions.assertTrue(i.available());
            Assertions.assertEquals(testDefaultValue, script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.clear();
            Assertions.assertFalse(i.available());
            Assertions.assertSame(i.value(), testDefaultValue);
            Assertions.assertNull(script.teaseLib.persistence.get(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name)));
            i.set(testValue);
            script.teaseLib.persistence.clear(QualifiedName.of(TeaseLib.DefaultDomain, script.namespace, name));
            Assertions.assertFalse(i.available());
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
            Assertions.assertEquals(Fruit.Banana, myFruit.value());
            myFruit.set(Fruit.Apple);
            Assertions.assertEquals(Fruit.Apple, myFruit.value());
        }
    }
}
