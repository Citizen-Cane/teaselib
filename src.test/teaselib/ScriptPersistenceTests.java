package teaselib;

import static org.junit.Assert.*;

import org.junit.Test;

import teaselib.core.TeaseLib;
import teaselib.core.TeaseLib.PersistentEnum;
import teaselib.test.TestScript;

public class ScriptPersistenceTests {

    private enum TestValuesEnumClass {
        My_Test_Value_toy,
        My_Test_Value_set_by_name,
        My_Test_Value_set_by_enum,
        My_Test_Value_item_by_name,
        My_Test_Value_item_by_enum,
    }

    @Test
    public void testScriptPersistence() {
        TestScript script = TestScript.getOne();

        script.item("My test namespace.My Test value toy").setAvailable(true);
        assertTrue(script.persistence.storage.containsKey("My test namespace.My Test value toy"));

        script.set(TestValuesEnumClass.My_Test_Value_set_by_name.name(), "Saved as local enum by name");
        assertTrue(script.persistence.storage.containsKey(script.namespace + ".My_Test_Value_set_by_name"));

        script.set(TestValuesEnumClass.My_Test_Value_set_by_enum, "Saved by local enum");
        assertTrue(script.persistence.storage
                .containsKey("ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_set_by_enum"));
    }

    @Test
    public void testItemAvailability() {
        TestScript script = TestScript.getOne();

        script.item(TestValuesEnumClass.My_Test_Value_item_by_enum).setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey("ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum"));

        script.item("My test namespace.My Test Value item").setAvailable(true);
        assertTrue(script.persistence.storage.containsKey("My test namespace.My Test Value item"));
    }

    @Test
    public void testDomainSeparation() {
        TestScript script = TestScript.getOne();

        script.item(Clothes.Underpants).setAvailable(true);
        assertTrue(script.item(Clothes.Underpants).isAvailable());
        // TODO Support domain names for user items
        assertFalse("Item domains not separated", script.domain(Clothes.Doll).item(Clothes.Underpants).isAvailable());

        script.domain(Clothes.Doll).item(Clothes.Underpants).setAvailable(true);
        assertTrue(script.domain(Clothes.Doll).item(Clothes.Underpants).isAvailable());

        assertEquals(2, script.persistence.storage.size());
    }

    @Test
    public void testSetVersusItem() {
        TestScript script = TestScript.getOne();

        script.item(TestValuesEnumClass.My_Test_Value_item_by_enum).setAvailable(false);
        assertEquals(false, script.getBoolean(TestValuesEnumClass.My_Test_Value_item_by_enum));

        script.item(TestValuesEnumClass.My_Test_Value_item_by_enum).setAvailable(true);
        assertEquals(true, script.getBoolean(TestValuesEnumClass.My_Test_Value_item_by_enum));

        script.set(TestValuesEnumClass.My_Test_Value_item_by_enum, false);
        assertEquals(false, script.item(TestValuesEnumClass.My_Test_Value_item_by_enum).isAvailable());

        script.set(TestValuesEnumClass.My_Test_Value_item_by_enum, true);
        assertEquals(true, script.item(TestValuesEnumClass.My_Test_Value_item_by_enum).isAvailable());

        assertEquals(1, script.persistence.storage.size());
    }

    @Test
    public void testScriptPersistencenGlobalNamespace() {
        TestScript script = TestScript.getOne();

        script.teaseLib.set(TeaseLib.DefaultDomain, "My domain", TestValuesEnumClass.My_Test_Value_set_by_name.name(),
                "Saved by global name");
        assertTrue(script.persistence.storage.containsKey("My domain.My_Test_Value_set_by_name"));

        script.teaseLib.set("My domain", TestValuesEnumClass.My_Test_Value_set_by_enum, "Saved by global enum");
        assertTrue(script.persistence.storage
                .containsKey("My domain.ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_set_by_enum"));

        script.teaseLib
                .item(TeaseLib.DefaultDomain,
                        "My namespace" + "." + TestValuesEnumClass.My_Test_Value_item_by_name.name())
                .setAvailable(true);
        assertTrue(script.persistence.storage.containsKey("My namespace.My_Test_Value_item_by_name"));

        script.teaseLib.item(TeaseLib.DefaultDomain, TestValuesEnumClass.My_Test_Value_item_by_enum).setAvailable(true);
        assertTrue(script.persistence.storage
                .containsKey("ScriptPersistenceTests.TestValuesEnumClass.My_Test_Value_item_by_enum"));
    }

    @Test
    public void testPersistentEnum() {
        TestScript script = TestScript.getOne();

        PersistentEnum<TestValuesEnumClass> testValue = script.persistentEnum("test", TestValuesEnumClass.class);
        assertFalse(testValue.available());

        for (TestValuesEnumClass value : TestValuesEnumClass.values()) {
            testValue.set(value);
            assertEquals(value, testValue.value());
            script.persistence.printStorage();
        }

        assertTrue(testValue.available());
        testValue.clear();
        assertFalse(testValue.available());
    }
}
