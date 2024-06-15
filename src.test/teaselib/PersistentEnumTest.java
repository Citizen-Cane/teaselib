package teaselib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import teaselib.core.TeaseLib;
import teaselib.core.util.QualifiedName;
import teaselib.test.TestScript;

import java.io.IOException;

public class PersistentEnumTest {

    enum FoobarBool implements PersistentEnum.Bool {
        Flag()
    }

    @Test
    public void testBool() throws IOException {
        try (TestScript script = new TestScript()) {
            var flag = script.persistence.newBoolean(FoobarBool.Flag);
            Assertions.assertFalse(flag.value());
            Assertions.assertFalse(script.persistence.value(FoobarBool.Flag));
            flag.set();
            Assertions.assertTrue(script.persistence.value(FoobarBool.Flag));
            Assertions.assertTrue(flag.value());
        }
    }

    enum FoobarInt implements PersistentEnum.Number {
        Flag
    }

    @Test
    public void testNumber() throws IOException {
        try (TestScript script = new TestScript()) {
            var flag = script.persistence.newNumber(FoobarInt.Flag);
            Assertions.assertEquals(0, flag.value().intValue());
            flag.defaultValue(42L);
            Assertions.assertEquals(42, flag.value().intValue());
            script.persistence.set(FoobarInt.Flag, 666);
            Assertions.assertEquals(666, flag.value().intValue());
            Assertions.assertEquals(666, script.persistence.value(FoobarInt.Flag).intValue());
            flag.defaultValue(42);
            Assertions.assertEquals(666, flag.value().intValue());
            flag.clear();
            Assertions.assertEquals(42, flag.value().intValue());
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
            Assertions.assertTrue(script.storage.isEmpty());

            a.set();
            b.set(100);
            Assertions.assertEquals(2, script.storage.size());
            Assertions.assertEquals("true", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "PersistentEnumTest.FoobarVariant", "Flag1")));
            Assertions.assertEquals("100", script.storage.get(QualifiedName.of(TeaseLib.DefaultDomain, "PersistentEnumTest.FoobarVariant", "Flag2")));
            Assertions.assertTrue(a.value().booleanValue());
            Assertions.assertEquals(100, b.value().intValue());
            Assertions.assertEquals(100, b.value().longValue());
        }
    }

    @Test
    public void testPersistentValuesAreSingletons() throws IOException {
        try (TestScript script = new TestScript()) {
            var a = script.persistence.newBoolean(FoobarVariant.Flag1);
            var b = script.persistence.newBoolean(FoobarVariant.Flag1);
            Assertions.assertSame(a, b);
        }
    }
}
