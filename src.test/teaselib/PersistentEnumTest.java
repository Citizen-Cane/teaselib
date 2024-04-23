package teaselib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

public class PersistentEnumTest {

    enum FoobarBool implements PersistentEnum.Bool {
        Flag()
    }

    @Test
    public void testBool() throws IOException {
        try (TestScript script = new TestScript()) {
            var flag = script.persistence.newBoolean(FoobarBool.Flag);
            assertFalse(flag.value());
            assertFalse(script.persistence.value(FoobarBool.Flag));
            flag.set();
            assertTrue(script.persistence.value(FoobarBool.Flag));
            assertTrue(flag.value());
        }
    }

    enum FoobarInt implements PersistentEnum.Number {
        Flag
    }

    @Test
    public void testNumber() throws IOException {
        try (TestScript script = new TestScript()) {
            var flag = script.persistence.newNumber(FoobarInt.Flag);
            assertEquals(0, flag.value().intValue());
            flag.defaultValue(42L);
            assertEquals(42, flag.value().intValue());
            script.persistence.set(FoobarInt.Flag, 666);
            assertEquals(666, flag.value().intValue());
            assertEquals(666, script.persistence.value(FoobarInt.Flag).intValue());
            flag.defaultValue(42);
            assertEquals(666, flag.value().intValue());
            flag.clear();
            assertEquals(42, flag.value().intValue());
        }
    }

    enum FoobarVariant {
        Flag1,
        Flag2
    }

    @Test
    public void testVariantType() throws IOException {
        try (TestScript script = new TestScript()) {
            var a = script.persistence.newBoolean(FoobarVariant.Flag1);
            var b = script.persistence.newNumber(FoobarVariant.Flag2);
            assertTrue(script.storage.isEmpty());

            a.set();
            b.set(100);
            assertEquals(2, script.storage.size());
            assertEquals("true", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "PersistentEnumTest.FoobarVariant", "Flag1")));
            assertEquals("100", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "PersistentEnumTest.FoobarVariant", "Flag2")));
            assertEquals(true, a.value().booleanValue());
            assertEquals(100, b.value().intValue());
            assertEquals(100, b.value().longValue());
        }
    }

    @Test
    public void testPersistentValuesAreSingletons() throws IOException {
        try (TestScript script = new TestScript()) {
            var a = script.persistence.newBoolean(FoobarVariant.Flag1);
            var b = script.persistence.newBoolean(FoobarVariant.Flag1);
            assertSame(a, b);
        }
    }
}
